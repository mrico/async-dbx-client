/*
The MIT License (MIT)

Copyright (c) 2013 Marco Rico Gomez <http://mrico.eu>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package asyncdbx

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardOpenOption._
import java.nio.channels.WritableByteChannel
import scala.util.{ Try, Failure }
import scala.concurrent.duration._
import akka.actor._
import akka.event.Logging
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import api._

private[asyncdbx] object DbxSyncOp {
  case object Ack
  case object Alive
  case class CreateFolder(meta: Data.Metadata)
  case class CreateFile(meta: Data.Metadata)
  case class Delete(path: String)

  class DbxSyncOpFailed(msg: String) extends RuntimeException(msg)

  def props(dbxClient: ActorRef, baseDir: File): Props = {
    Props(classOf[DbxSyncOp], dbxClient, baseDir)
  }
}

private[asyncdbx] class DbxSyncOp(dbxClient: ActorRef, baseDir: File) extends Actor {
  import DbxDownloader._
  import DbxSyncOp._

  val log = Logging(context.system, this)

  def receive: Receive = {
    case CreateFolder(meta) =>
      val folder = new File(baseDir, meta.path)
      if (folder.exists || folder.mkdirs()) {
        context.sender ! Ack
        context.stop(self)
      } else {
        throw new DbxSyncOpFailed(s"Cannot create folder: ${folder}")
      }

    case CreateFile(meta) =>
      context.become(downloading(context.sender, meta))
      dbxClient ! Data.GetFile("sandbox", meta.path)

    case Delete(path) =>
      delete(new File(baseDir, path))
      context.sender ! Ack
      context.stop(self)
  }

  def downloading(receiver: ActorRef, meta: Data.Metadata, channel: Option[WritableByteChannel] = None): Receive = {
    case DownloadBegin(path) =>
      log.info("Downloading {} ...", path)
      val file = resolveFile(new File(baseDir, path), isNew = true).get
      val channel = Files.newByteChannel(file.toPath, CREATE, TRUNCATE_EXISTING, WRITE)
      context.become(downloading(receiver, meta, Some(channel)))

    case DownloadChunk(path, data) =>
      channel.foreach(_.write(ByteBuffer.wrap(data)))
      receiver ! Alive

    case DownloadEnd(path) =>
      channel.foreach(_.close())
      val f = resolveFile(new File(baseDir, path)).get
      if (f.length == meta.bytes) {
        log.info("Download completed for {}.", path)
        receiver ! Ack
        context.stop(self)
      } else {
        throw new DbxSyncOpFailed(s"${path}: Size differs - expected: ${meta.bytes} got ${f.length}")
      }
  }

  def delete(file: File) {
    log.info("Delete: {}", file)
    if (! file.exists) {
      resolveFile(file).foreach(delete)

    } else if (file.isDirectory) {
      file.listFiles foreach delete

      if (! file.delete()) {
        throw new DbxSyncOpFailed(s"Cannot delete folder: ${file}")
      }

    } else if (file.isFile) {
       if (! file.delete()) {
        throw new DbxSyncOpFailed(s"Cannot delete file: ${file}")
      }
    }
  }

  // "Note: Dropbox treats file names in a case-insensitive but case-preserving way"
  def resolveFile(file: File, isNew: Boolean = false): Option[File] = {
    if (file.exists) {
      Some(file)
    } else {
      val lowerFileName = file.getName.toLowerCase
      val parentFile = resolveFile(file.getParentFile)
      val files = parentFile.flatMap(f => Option(f.listFiles))
      val caseInsensitiveCompare = (f: File) => f.getName.toLowerCase == lowerFileName
      files.flatMap(_.find(caseInsensitiveCompare)) match {
        case None if isNew => parentFile.map(new File(_, file.getName))
        case x => x
      }
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    // retry
    message foreach { self forward _ }
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1.minute) {
      case _: DbxDownloader.DbxDownloadFailed => Escalate
      case _: Exception => Escalate
  }
}