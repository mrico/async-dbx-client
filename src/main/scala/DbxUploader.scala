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

object DbxUploader {
  def props(token: String): Props = {
    Props(classOf[DbxUploader], token)
  }
}

private[asyncdbx] class DbxUploader(token: String) extends Actor with DbxApiCalls {
  import DbxJsonSupport._
  import api._

  val settings = Settings(context.system)
  import context.dispatcher

  def receive: Receive = {
    case Data.UploadFile(localFile, root, path, overwrite, parentRev) =>
      import Data.JsonProtocol._
      val sender = context.sender
      val pipeline = sendReceive ~> unmarshal[Data.Metadata]
      val params = Map(
        "overwrite" -> overwrite.toString,
        "parent_rev" -> parentRev.getOrElse("")
      )
      val request = AuthPut(token, settings.ContentUri,
        s"/${settings.ApiVersion}/files_put/${root}/${path}",
        HttpData(localFile), params)

      pipeline(request) onComplete {
        case Success(meta) =>
          sender ! Data.FileUploaded(meta.asInstanceOf[Data.Metadata])
        case Failure(ex) =>
          sender ! ex
      }
  }
}