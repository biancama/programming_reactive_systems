package com.biancama.linkchecker.actors

import akka.actor.{Actor, Props, ReceiveTimeout}
import com.biancama.linkchecker.AsyncWebClient

import scala.concurrent.duration.DurationInt

object LinkCheckerMain {

  case object Start

  def props = Props[LinkCheckerMain]

}



class LinkCheckerMain extends Actor {
  import LinkCheckerMain._

  context.setReceiveTimeout(10 seconds)

  def receive = idle

  def idle: Receive ={
    case Start =>
      val receptionist = context.actorOf(Receptionist.props, "receptionist")

      receptionist ! Receptionist.Get("http://www.google.com")
      receptionist ! Receptionist.Get("http://www.google.com/1")
      receptionist ! Receptionist.Get("http://www.google.com/2")
      receptionist ! Receptionist.Get("http://www.google.com/3")

      context.become(running)
  }

  def running: Receive = {
    case Receptionist.Result(url, links) =>
      println(links.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))
    case Receptionist.Failed(url) =>
      println(s"Failed to fetch '$url'\n")
    case ReceiveTimeout =>
      context.stop(self)
  }

  override def postStop(): Unit =
    AsyncWebClient.shutdown()
}