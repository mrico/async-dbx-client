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

import spray.http._
import spray.client.pipelining._
import HttpMethods._
import HttpHeaders._

private[asyncdbx] trait DbxApiCalls {
  def settings: Settings

  object AuthGet {
    def apply(token: String, path: String) = {
      val headers = List(authHeader(token))
      Get(endpoint(path)).withHeaders(headers)
    }

    def apply(token: String, base: Uri, path: String, query: Map[String, String] = Map.empty) = {
      val uri = Uri.from(path = path).resolvedAgainst(base).withQuery(query)
      val headers = List(authHeader(token))
      Get(uri).withHeaders(headers)
    }
  }

  object AuthPost {
    def apply(token: String, path: String, data: Map[String, String] = Map.empty) = {
      val headers = List(authHeader(token))
      Post(endpoint(path), FormData(data)).withHeaders(headers)
    }
  }

  def endpoint(path: String) = {
    val p = Uri.from(path = s"/${settings.ApiVersion}${path}")
    p.resolvedAgainst(settings.ApiUri)
  }

  def authHeader(token: String) = RawHeader("Authorization", s"Bearer ${token}")
}
