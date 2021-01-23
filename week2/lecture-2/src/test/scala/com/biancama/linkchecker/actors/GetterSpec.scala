package com.biancama.linkchecker.actors
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import com.biancama.linkchecker.actors.Getter.Done
import com.biancama.linkchecker.{BadStatus, WebClient}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.concurrent.Executor
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class StepParent(child: Props, fwd: ActorRef) extends Actor {
  context.watch(context.actorOf(child, "child"))
  def receive = {
    case Terminated(_) => context.stop(self)
    case msg           => fwd.tell(msg, sender)
  }
}

object GetterSpec {

  val firstLink = "http://www.rkuhn.info/1"

  val bodies = Map(
    firstLink ->
      """<html>
        |  <head><title>Page 1</title></head>
        |  <body>
        |    <h1>A Link</h1>
        |   <a href="http://rkuhn.info/2">click here</a>
        |  </body>
        |</html>""".stripMargin)

  val links = Map(
    firstLink -> Seq("http://rkuhn.info/2"))

  object FakeWebClient extends WebClient {

    override def get(url: String)(implicit exec: Executor): Future[String] = bodies get url match {
      case None       => Future.failed(BadStatus(404))
      case Some(body) => Future.successful(body)
    }
  }

  def fakeGetter(url: String, depth: Int): Props =
    Props(new Getter(url, depth) {
      override def client = FakeWebClient
    })

}

class GetterSpec extends TestKit(ActorSystem("GetterSpec"))
  with AnyWordSpecLike with BeforeAndAfterAll with ImplicitSender {

  import GetterSpec._

  override def afterAll(): Unit = {
    concurrent.Await.result(system.terminate(), Duration.Inf)
    ()
  }

  "A Getter" must {

    "return the right body" in {
      val getter = system.actorOf(Props(new StepParent(fakeGetter(firstLink, 2), testActor)), "rightBody")
      for (link <- links(firstLink))
        expectMsg(Controller.Check(link, 2))
      watch(getter)
      expectMsg(Done)
      expectTerminated(getter)
    }

    "properly finish in case of errors" in {
      val getter = system.actorOf(Props(new StepParent(fakeGetter("unknown", 2), testActor)), "wrongLink")
      watch(getter)
      expectMsg(Done)
      expectTerminated(getter)
    }

  }

}