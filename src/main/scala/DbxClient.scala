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

import scala.concurrent.Future
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
import spray.client.pipelining._
import spray.httpx.UnsuccessfulResponseException
import HttpMethods._
import HttpHeaders._
import OAuthHandler._

/** Actor-based Dropbox API client.
  *
  * If you have a valid OAuth2 token, use the following method to authenticate
  * the client:
  * {{{
  * import api._
  * val client = system.actorOf(Props[DbxClient])
  * client ! Authenticate(token) // => Response: Autenticated
  * }}}
  */
class DbxClient extends Actor with DbxApiCalls {
  import DbxJsonSupport._
  import api._
  import context.dispatcher

  val settings = Settings(context.system)
  val log = Logging(context.system, this)

  def receive: Receive = {
    case Authorized(code, state) =>
      log.debug("Retrieved autorization request: state={}, code={}", state, code)
      self ! OAuth2.GetToken(code, (settings.AppKey, settings.AppSecret), settings.OAuth2RedirectUri)

    case OAuth2.GetToken(code, (appKey, appSecret), redirectUri) =>
      import OAuth2.JsonProtocol._

      val sender = context.sender
      val pipeline = sendReceive ~> unmarshal[OAuth2.Token]
      val formData = FormData(Map(
        "code" -> code,
        "grant_type" -> "authorization_code",
        "client_id" -> appKey,
        "client_secret" -> appSecret,
        "redirect_uri" -> redirectUri.toString
      ))
      val responseFuture = pipeline { Post(endpoint("/oauth2/token"), formData) }
      responseFuture onComplete {
        case Success(OAuth2.Token(token, _, _)) =>
          log.debug("Retrieved oauth2 token: {}", token)
          context.become(authenticated(token))
          sender ! Authenticated

        case Failure(error) =>
          sender ! error
      }

    case Authenticate(token) =>
      context.become(authenticated(token))
      context.sender ! Authenticated

    case _: DbxRequest =>
      context.sender ! NotAuthenticated
  }

  def authenticated(token: String): Receive = {
    case Account.GetInfo =>
      import Account.JsonProtocol._

      val pipeline = sendReceive ~> unmarshal[Account.Info]
      val responseFuture = pipeline { AuthGet(token, "/account/info") }
      responseFuture onComplete reply(context.sender)

    case Data.GetDelta(cursorOption, pathPrefix) =>
      import Data.JsonProtocol._

      val pipeline = sendReceive ~> unmarshal[Data.Delta]
      var data = Map.empty[String, String]
      cursorOption.foreach(data += "cursor" -> _)
      pathPrefix.foreach(data += "path_prefix" -> _)
      val responseFuture = pipeline { AuthPost(token, "/delta", data) }
      responseFuture onComplete reply(context.sender)

    case Data.GetLongpollDelta(cursor) =>
      import Data.JsonProtocol._

      implicit val system = context.system
      implicit val timeout: Timeout = 90.seconds
      val sender = context.sender
      val httpSettings = {
        val hcs = HostConnectorSettings(system)
        val cs = hcs.connectionSettings.copy(requestTimeout = 90.seconds)
        hcs.copy(connectionSettings = cs)
      }
      val query = Map("cursor" -> cursor)
      val request = AuthGet(token, settings.NotifyUri,
          s"/${settings.ApiVersion}/longpoll_delta", query)

      val pipeline =
        for (
          Http.HostConnectorInfo(connector, _) <-
            IO(Http) ? Http.HostConnectorSetup(settings.NotifyUri.authority.host.toString,
              port = settings.NotifyUri.effectivePort,
              sslEncryption = true, settings = Some(httpSettings))
        ) yield sendReceive(connector) ~> unmarshal[Data.LongpollDelta]

      val responseFuture = pipeline.flatMap(_(request))
      responseFuture onComplete reply(sender)

    case Data.GetMetadata(root, path) =>
      import Data.JsonProtocol._

      val pipeline = sendReceive ~> unmarshal[Data.Metadata]
      val responseFuture = pipeline { AuthGet(token, s"/metadata/${root}/${path}") }
      responseFuture onComplete reply(context.sender)

    case msg @ Data.GetFile(_, _, _) =>
      val downloader = context.actorOf(DbxDownloader.props(token))
      downloader forward msg

    case FileOps.CreateFolder(root, path) =>
      import Data.JsonProtocol._

      val pipeline = sendReceive ~> unmarshal[Data.Metadata]
      val params = Map(
        "root" -> root,
        "path" -> path
      )
      val responseFuture = pipeline { AuthPost(token, "/fileops/create_folder", params) }
      val receiver = context.sender
      responseFuture onComplete {
        case Success(meta) =>
          receiver ! FileOps.FolderCreated(meta)

        case Failure(ex: UnsuccessfulResponseException) =>
          if (ex.response.status == StatusCodes.Forbidden) {
            receiver ! FileOps.AlreadyExists(path)
          } else {
            receiver ! ex
          }

        case Failure(ex) =>
          receiver ! ex
      }

    case FileOps.Delete(root, path) =>
      import Data.JsonProtocol._

      val pipeline = sendReceive ~> unmarshal[Data.Metadata]
      val params = Map(
        "root" -> root,
        "path" -> path
      )
      val responseFuture = pipeline { AuthPost(token, "/fileops/delete", params) }
      val receiver = context.sender
      responseFuture onComplete {
        case Success(meta) =>
          receiver ! FileOps.Deleted(meta)

        case Failure(ex: UnsuccessfulResponseException) =>
          if (ex.response.status == StatusCodes.NotFound) {
            receiver ! FileOps.NotFound(path)
          } else {
            receiver ! ex
          }

        case Failure(ex) =>
          receiver ! ex
      }

    case OAuth2.DisableAccessToken =>
      val sender = context.sender
      val pipeline = sendReceive
      val responseFuture = pipeline { AuthPost(token, "/disable_access_token") }
      responseFuture onComplete {
        case Success(_) =>
          context.unbecome()
          sender ! OAuth2.AccessTokenDisabled
        case Failure(err) =>
          sender ! err
      }
  }

  def reply(receiver: ActorRef): PartialFunction[Try[Any], Unit] = {
    case Success(data) =>
      receiver ! data

    case Failure(ex: UnsuccessfulResponseException) =>
      log.error(ex, "Got unsuccessful response from Dropbox.")
      val httpStatus = ex.response.status
      if (httpStatus == StatusCodes.Unauthorized) {
        context.unbecome()
      } else {
        receiver ! ex
      }

    case Failure(ex) =>
      log.error("", ex)
      receiver ! ex
  }
}
