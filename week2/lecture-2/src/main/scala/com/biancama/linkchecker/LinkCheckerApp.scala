package com.biancama.linkchecker

import akka.actor.ActorSystem
import com.biancama.linkchecker.actors.LinkCheckerMain

object LinkCheckerApp extends App {

  val system = ActorSystem("linkCheckerSystem")

  val master = system.actorOf(LinkCheckerMain.props, "master")

  master ! LinkCheckerMain.Start

}
