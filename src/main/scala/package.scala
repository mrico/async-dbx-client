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

/** Akka/Spray-based Dropbox API client.
  *
  * The main entry point is the [[DbxClient]]. An instance of [[DbxClient]]
  * is linked to a single Dropbox account and provides access to the API.
  * {{{
  * import api._
  * val client = system.actorOf(Props[DbxClient])
  * client ! Authenticate(token)
  * client ! Account.GetInfo // => Response: Account.Info
  * }}}
  *
  * The actor [[DbxSync]] can be used to keep a local folder in sync with
  * a given Dropbox account.
  * {{{
  * val sync = system.actorOf(DbxSync.props(client, "/tmp/dbx"))
  * // ... later ...
  * sync ! PoisenPoll
  * }}}
  *
  * @author Marco Rico Gomez <http://mrico.eu>
  * @version 0.1
  * @see [[http://akka.io akka.io]]
  * @see [[http://spray.io spray.io]]
  */
package object asyncdbx
