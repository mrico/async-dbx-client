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

import java.io.File
import spray.json.{JsonFormat, DefaultJsonProtocol}

/** Container for Data related messages. */
object Data {
  case class UploadFile(
    localFile: File,
    root: String,
    path: String,
    overwrite: Boolean = true,
    prarentRev: Option[String] = None
  )
  case class FileUploaded(meta: Data.Metadata)

  /** Downloads a file.
    *
    * @param root 'dropbox' or 'sandbox'
    * @param path the path to the file to download
    * @param rev the revision of the file (defaults to latest)
    * @see [[https://www.dropbox.com/developers/core/docs#files-GET]]
    */
  case class GetFile(root: String, path: String, rev: Option[String] = None) extends DbxRequest

  case class DownloadBegin(path: String)
  case class DownloadChunk(path: String, data: Array[Byte])
  case class DownloadEnd(path: String)

  class DbxDownloadFailed(cause: Throwable) extends RuntimeException(cause)

  /** Retrieve information about changed files and folders.
    *
    * As a response you will get a [[Delta]] message.
    *
    * @param cursor current state
    * @param pathPrefix only return changes at or under the specified path
    * @see [[https://www.dropbox.com/developers/core/docs#delta]]
    */
  case class GetDelta(cursor: Option[String], pathPrefix: Option[String] = None) extends DbxRequest

  case class Delta(
    cursor: String,
    has_more: Boolean,
    reset: Option[Boolean],
    entries: List[(String, Option[Metadata])]
  )

  // https://www.dropbox.com/developers/core/docs#longpoll-delta
  case class GetLongpollDelta(cursor: String) extends DbxRequest

  case class LongpollDelta(changes: Boolean, backoff: Option[Int])

  // https://www.dropbox.com/developers/core/docs#metadata
  // TODO: Parameters
  case class GetMetadata(
    root: String,
    path: String
  ) extends DbxRequest

  case class Metadata(
    size: String,
    bytes: Long,
    path: String,
    is_dir: Boolean,
    is_deleted: Option[Boolean],
    rev: Option[String],
    hash: Option[String],
    thumb_exists: Boolean,
    icon: String,
    modified: Option[String],
    client_mtime: Option[String],
    root: String,
    mimeType: Option[String],
    contents: Option[List[Metadata]]
  )

  object JsonProtocol extends DefaultJsonProtocol {
    private val metadataJsonFormat: JsonFormat[Metadata] = lazyFormat(jsonFormat14(Metadata))
    implicit val metadata = rootFormat(metadataJsonFormat)
    implicit val deltaFormat = jsonFormat4(Delta)
    implicit val longpollDelta = jsonFormat2(LongpollDelta)
  }
}
