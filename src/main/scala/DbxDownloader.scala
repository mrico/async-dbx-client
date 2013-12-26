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

import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

import akka.actor._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import akka.event.Logging
import spray.http._
import spray.can.Http
import spray.can.client._
import spray.client._
import spray.client.pipelining._
import spray.httpx.UnsuccessfulResponseException
import spray.json.{JsonFormat, DefaultJsonProtocol}
import HttpMethods._
import HttpHeaders._


private[asyncdbx] object DbxDownloader {
  case class DownloadBegin(path: String)
  case class DownloadChunk(path: String, data: Array[Byte])
  case class DownloadEnd(path: String)

  class DbxDownloadFailed(cause: Throwable) extends RuntimeException(cause)

  def props(token: String): Props = {
    Props(classOf[DbxDownloader], token)
  }
}

private[asyncdbx] class DbxDownloader(token: String) extends Actor with DbxApiCalls {
  import api._
  import DbxDownloader._
  import context.dispatcher

  implicit val system = context.system
  val settings = Settings(system)
  val log = Logging(system, this)
  val httpSettings = {
    val hcs = HostConnectorSettings(system)
    val cs = hcs.connectionSettings.copy(responseChunkAggregationLimit = 0, requestTimeout = Duration.Inf)
    hcs.copy(connectionSettings = cs)
  }

  def receive: Receive = {
    case Data.GetFile(root, path, rev) =>
      context.become(downloading(context.sender, path))

      implicit val timeout: Timeout = 10.seconds
      val params = Map("rev" -> rev.getOrElse(""))
      val req = AuthGet(token, settings.ContentUri, s"/${settings.ApiVersion}/files/${root}${path}", params)
      for (
        Http.HostConnectorInfo(connector, _) <-
          IO(Http) ? Http.HostConnectorSetup(settings.ContentUri.authority.host.toString,
            port = settings.ContentUri.effectivePort,
            sslEncryption = true, settings = Some(httpSettings))
      ) yield {
        sendTo(connector).withResponsesReceivedBy(self)(req)
      }
  }

  def downloading(requestor: ActorRef, path: String): Receive = {
    case ChunkedResponseStart(_) =>
      requestor ! DownloadBegin(path)

    case MessageChunk(entity, _) =>
      requestor ! DownloadChunk(path, entity.toByteArray)

    case HttpResponse(status, entity, _, _) =>
      requestor ! DownloadBegin(path)
      requestor ! DownloadChunk(path, entity.data.toByteArray)
      requestor ! DownloadEnd(path)
      context.stop(self)

    case ChunkedMessageEnd(_, _) =>
      requestor ! DownloadEnd(path)
      context.stop(self)

    case Failure(ex) =>
      throw new DbxDownloadFailed(ex)
  }
}
