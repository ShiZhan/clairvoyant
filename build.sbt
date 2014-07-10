name := "clairvoyant"

version := "1.0"

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.4",
  "commons-codec" % "commons-codec" % "1.8",
  "commons-validator" % "commons-validator" % "1.4.0",
  "org.jsoup" % "jsoup" % "1.7.3",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "log4j" % "log4j" % "1.2.17"
)
