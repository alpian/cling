name := """cling"""

version := "1.0"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "io.netty" % "netty-all" % "4.0.32.Final",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe" % "config" % "1.3.0")

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

