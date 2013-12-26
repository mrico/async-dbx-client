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
package api

import spray.http.Uri
import spray.json.{JsonFormat, DefaultJsonProtocol}

/** Container for OAuth2 related messages. */
object OAuth2 {
  type AppKeySecret = (String, String)

  // https://www.dropbox.com/developers/core/docs#oa2-token
  case class GetToken(
    code: String,
    appKeySecret: AppKeySecret,
    redirectUri: Uri
  ) extends DbxRequest

  case class Token(
    access_token: String,
    token_type: String,
    uid: String
  )

  // https://www.dropbox.com/developers/core/docs#disable-token
  case object DisableAccessToken extends DbxRequest
  case object AccessTokenDisabled

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val tokenFormat = jsonFormat3(Token)
  }
}
