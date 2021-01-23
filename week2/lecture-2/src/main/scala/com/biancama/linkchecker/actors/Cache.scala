package com.biancama.linkchecker.actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import com.biancama.linkchecker.AsyncWebClient
import com.biancama.linkchecker.actors.Cache.{Get, Result}


object Cache {

  case class Get(url: String)
  case class Result(client: ActorRef, url: String, body: String)

}

class Cache extends Actor {

  implicit val exec = context.dispatcher

  var cache = Map.empty[String, String]
  override def receive: Receive = {
    case Get(url) =>
      if (cache.contains(url)) sender ! cache(url)
      else {
        val client = sender()
        val webClient = AsyncWebClient
        webClient get url map (Result (client, url, _)) pipeTo self
      }
    case Result(client, url, body) =>
      cache += url -> body
      client ! body
  }
}
