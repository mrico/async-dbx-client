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

import asyncdbx._
import asyncdbx.api._

class DbxClientSpec extends TestKit(ActorSystem("DbxClientSpec")) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  def newClient = system.actorOf(Props[DbxClient])

  "A DbxClient" should {
    "confirm a 'Authenticate' message with a 'Authenticated' response" in {
      val client = newClient
      client ! Authenticate("my-spec-token")
      expectMsg(Authenticated)
    }

    "respond with a 'NotAuthenticated' message when receives a API call in unauthenticated state" in {
      val client = newClient
      client ! Account.GetInfo
      expectMsg(NotAuthenticated)
    }
  }

  override def afterAll() {
    shutdown(system)
  }
}