package com.biancama.linkchecker.actors

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import com.biancama.linkchecker.{AsyncWebClient, LinksFinder, WebClient}


object Getter {

  case object Done
  case object Abort

  def props(url: String, depth: Int) = Props(new Getter(url, depth))

}
class Getter(url: String, depth: Int) extends Actor {

  import Getter._

  implicit val exec = context.dispatcher
  def client: WebClient = AsyncWebClient

  client get(url) pipeTo self
  /*
    val future = client.get(url)
    future pipeTo self


    future onComplete({
      case Success(body) => self ! body
      case Failure(err) => self ! Status.Failure(err)
    })
    this is equivalent to pipeTo

     */
  def receive = {
    case body: String =>
      for (link <- LinksFinder.find(body))
        context.parent ! Controller.Check(link, depth)
      stop()
    case Abort => stop()
    case _ => stop() //see Status.Failure

  }

  def stop() = {
    context.parent ! Done
    context.stop(self)
  }

}