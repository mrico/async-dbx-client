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

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import com.typesafe.config.Config
import spray.http.Uri
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class Settings(config: Config) extends Extension {
  val ApiVersion: String = config.getString("dropbox.api-version")
  val AppKey: String = config.getString("dropbox.app-key")
  val AppSecret: String = config.getString("dropbox.app-secret")
  val OAuth2RedirectUri: Uri = Uri(config.getString("dropbox.oauth2.redirect-uri"))
  val SyncTimeout: Duration =
    Duration(config.getMilliseconds("dropbox.sync-timeout"),
    TimeUnit.MILLISECONDS)
  val ApiUri: Uri = Uri(config.getString("dropbox.endpoints.api-uri"))
  val NotifyUri: Uri = Uri(config.getString("dropbox.endpoints.notify-uri"))
  val ContentUri: Uri = Uri(config.getString("dropbox.endpoints.content-uri"))
}

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new Settings(system.settings.config)

  override def get(system: ActorSystem): Settings = super.get(system)
}
