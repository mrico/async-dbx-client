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
import scala.concurrent.duration._
import akka.actor._
import akka.event.Logging
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

object DbxSync {
  class DbxSyncFailed(msg: String) extends RuntimeException(msg)

  def props(dbxClient: ActorRef, path: String): Props = {
    Props(classOf[DbxSync], dbxClient, path)
  }
}

/** Keeps a local folder in sync with the provided Dropbox.
  *
  * DbxSync polls for changes and applies the deltas automatically until
  * the actor will be stopped.
  *
  * DbxSync must be linked to an authenticated [[DbxClient]].
  *
  * Example:
  * {{{
  *   val sync = system.actorOf(DbxSync.props(dbxClient, "/tmp/dbx"))
  * }}}
  */
class DbxSync(dbxClient: ActorRef, path: String) extends Actor {
  import api._
  import DbxSync._
  import DbxDownloader._
  import DbxSyncOp._
  import context.dispatcher

  val log = Logging(context.system, this)
  val settings = Settings(context.system)
  val baseDir = new File(path)
  var cursor: Option[String] = None

  context.setReceiveTimeout(settings.SyncTimeout)

  def receive: Receive = waiting

  val waiting: Receive = {
    case Data.LongpollDelta(changes, _) =>
      log.debug("LongpollDelta returned with changes={}", changes)
      if (changes) {
        dbxClient ! Data.GetDelta(cursor)
      } else {
        dbxClient ! Data.GetLongpollDelta(cursor.get)
      }

    case delta @ Data.Delta(cur, hasMore, reset, entries) =>
      log.debug("Requesting delta for cursor={}", cur)
      cursor = Some(cur)
      context.become(processing(entries.size, delta))
      entries foreach { case (path, metaOption) =>
        log.debug(metaOption.toString)
        metaOption match {
          case Some(meta) if meta.is_dir =>
            syncOp ! CreateFolder(meta)

          case Some(meta) if !meta.is_dir =>
            syncOp ! CreateFile(meta)

          case None =>
            syncOp ! Delete(path)
        }
      }

    case ReceiveTimeout =>
      cursor match {
        case Some(cur) =>  dbxClient ! Data.GetLongpollDelta(cur)
        case None => dbxClient ! Data.GetDelta(None)
      }
  }

  def processing(remainingAcks: Int, delta: Data.Delta): Receive = {
    case DbxSyncOp.Ack if (remainingAcks - 1) > 0 =>
      context.become(processing(remainingAcks - 1, delta))

    case DbxSyncOp.Ack =>
      log.info("Applied delta: {}", delta.cursor)
      context.become(waiting)
      if (delta.has_more) {
        dbxClient ! Data.GetDelta(cursor)
      } else {
        dbxClient ! Data.GetLongpollDelta(cursor.get)
      }

    case DbxSyncOp.Alive =>

    case ReceiveTimeout =>
      throw new DbxSyncFailed(s"Timeout received while applied delta ${delta.cursor}")
  }

  def syncOp: ActorRef = context.actorOf(DbxSyncOp.props(dbxClient, baseDir))

  override def preStart() {
    dbxClient ! Data.GetDelta(cursor)
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1.minute) {
      case _: DbxSyncOp.DbxSyncOpFailed => Restart
      case _: Exception => Escalate
  }
}
