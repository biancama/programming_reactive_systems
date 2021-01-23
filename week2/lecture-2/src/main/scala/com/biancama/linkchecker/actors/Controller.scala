package com.biancama.linkchecker.actors

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import com.biancama.linkchecker.actors.Controller.{Check, Result}

object Controller {
  case class Check(url: String, depth: Int)
  case class Result(links: Set[String])
  def props = Props[Controller]
}

class Controller extends Actor with ActorLogging {
  var cache = Set.empty[String]
  var children = Set.empty[ActorRef]

  /*


  context.system.scheduler.scheduleOnce(10.seconds) {
    children foreach(_ ! Getter.Abort)
  }

  this is not thread-safe
   */
  override def receive: Receive = {

    case Check(url: String, depth: Int) => {
      log.debug("{} checking {}", depth, url)
      if (!cache(url) && depth >0 ) {
        children += context.actorOf(Props(new Getter(url, depth - 1)))
      }
      cache += url
    }
    case Done => {
      children -= sender
      if (children.isEmpty) context.parent ! Result(cache)
    }
    case ReceiveTimeout => children foreach(_ ! Getter.Abort)
  }
}
