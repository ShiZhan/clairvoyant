name := "clairvoyant"

version := "1.0"

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.6")

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9",
  "commons-codec" % "commons-codec" % "1.10",
  "commons-validator" % "commons-validator" % "1.4.1",
  "org.jsoup" % "jsoup" % "1.8.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "log4j" % "log4j" % "1.2.17"
)
