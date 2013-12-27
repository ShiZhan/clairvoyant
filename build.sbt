name := "clairvoyant"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.10.3",
  "org.seleniumhq.webdriver" % "webdriver-common" % "0.9.7376",
  "org.seleniumhq.webdriver" % "webdriver-selenium" % "0.9.7376",
  "org.seleniumhq.webdriver" % "webdriver-htmlunit" % "0.9.7376",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "log4j" % "log4j" % "1.2.17"
)