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

import akka.actor.Actor
import akka.actor.ActorRef
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import spray.http._
import spray.can.Http
import HttpMethods._
import HttpHeaders._

object OAuthHandler {
  case class Subscribe(state: String, actor: ActorRef)
  case class Authorized(code: String, state: String)
}

class OAuthHandler extends Actor {
  import OAuthHandler._
  import context.system

  val settings = Settings(context.system)
  var subscriptions = Map.empty[String, ActorRef]

  def receive = {
    case Subscribe(state, actor) =>
      subscriptions += state -> actor

    case _: Http.Connected =>
      sender ! Http.Register(self)

    case req@HttpRequest(GET, settings.OAuth2RedirectUri.path, _, _, _) =>
      val query = req.uri.query
      sender ! HttpResponse(status = 200, entity = "Ok")
      for {
        code <- query.get("code")
        state <- query.get("state")
      } yield {
        subscriptions.get(state) foreach { subscriper =>
          subscriper ! Authorized(code, state)
          subscriptions -= state
        }
      }

    case _: HttpRequest =>
      sender ! HttpResponse(status = 404, entity = "Unknown resource!")
  }

  override def preStart() {
    IO(Http) ! Http.Bind(self, interface = "localhost", port = 8082)
  }

  override def postStop() {
    IO(Http) ! Http.Unbind
  }
}
