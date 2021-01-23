name := "lecture-2"

version := "0.1"

scalaVersion := "2.13.4"

val AkkaVersion = "2.6.10"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.4" % Test
libraryDependencies += "com.ning" % "async-http-client" % "1.9.40"
libraryDependencies += "org.jsoup" % "jsoup" % "1.13.1"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.10"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"