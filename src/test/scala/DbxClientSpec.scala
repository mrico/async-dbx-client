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
package tests

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory

import asyncdbx._
import asyncdbx.api._

object DbxClientSpec {
  val ConfigFile = new java.io.File("test-config.conf")
}

class DbxClientSpec extends TestKit(ActorSystem("DbxClientSpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  import DbxClientSpec._
  val client = system.actorOf(Props[DbxClient])
  val testFolder = s"/DbxClientSpec-${System.currentTimeMillis}"

  "An authenticated DbxClient" should {
    "be able to call 'account/info'" in {
      client ! Account.GetInfo
      val info = expectMsgType[Account.Info]
      info.uid should be > 0L
      info.referral_link should not be empty
      info.display_name should not be empty
      info.country should not be empty
    }

    "be able to call '/delta'" in {
      client ! Data.GetDelta(None)
      val delta = expectMsgType[Data.Delta]
      delta.reset shouldBe Some(true)
      delta.cursor should not be empty
      delta.entries should not be empty
      // next call with provided cursor
      client ! Data.GetDelta(Some(delta.cursor))
      val delta2 = expectMsgType[Data.Delta]
      delta2.entries shouldBe empty
      delta2.reset shouldBe Some(false)
    }

    "be able to call '/metadata' for the root folder" in {
      client ! Data.GetMetadata("sandbox", "/")
      val meta = expectMsgType[Data.Metadata]
      meta.path shouldBe "/"
      meta.is_dir shouldBe true
      meta.is_deleted shouldBe None
      meta.contents should not be empty
    }

    "be able to create a new folder" in {
      val msg = FileOps.CreateFolder("sandbox", testFolder)
      client ! msg
      val result = expectMsgType[FileOps.FolderCreated]
      val meta = result.meta
      meta.path shouldBe testFolder
      meta.is_dir shouldBe true
      // already exists
      client ! msg
      val exists = expectMsgType[FileOps.AlreadyExists]
      exists.path shouldBe testFolder
    }

    "be able to delete a folder" in {
      val msg = FileOps.Delete("sandbox", testFolder)
      client ! msg
      val result = expectMsgType[FileOps.Deleted]
      val meta = result.meta
      meta.path shouldBe testFolder
      meta.is_dir shouldBe true
      meta.is_deleted shouldBe Some(true)
      // not found
      client ! msg
      val notFound = expectMsgType[FileOps.NotFound]
      notFound.path shouldBe testFolder
    }
  }

  "An unauthenticated DbxClient" should {
    "respond with a 'NotAuthenticated' message when receives a API call" in {
      val client = system.actorOf(Props[DbxClient])
      client ! Account.GetInfo
      expectMsg(NotAuthenticated)
    }

    "confirm a 'Authenticate' message with a 'Authenticated' response" in {
      val client = system.actorOf(Props[DbxClient])
      client ! Authenticate("my-spec-token")
      expectMsg(Authenticated)
    }
  }

  override def beforeAll() {
    if (! ConfigFile.exists()) {
      sys.error(
        s"""|Config file '${ConfigFile.getAbsolutePath}' missing:
            |--- Contents: ---
            |tests {
            |  auth-token = "YOUR AUTH TOKEN"
            |}
            |-----------------
            |""".stripMargin
      )
    }
    val config = ConfigFactory.parseFile(ConfigFile)
    client ! Authenticate(config.getString("tests.auth-token"))
    expectMsg(Authenticated)
  }

  override def afterAll() {
    shutdown(system)
  }
}
