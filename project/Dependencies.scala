import sbt._

object Dependencies {

  // Logging
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"
  val slf4j_ext = "org.slf4j" % "slf4j-ext" % "1.7.12" withSources() withJavadoc()

  // Logback
  val logback_classic = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val logback_core = "ch.qos.logback" % "logback-core" % "1.1.3"

  // Testing
  val scalatest = "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

  /////////////////////////////////////////////////////////
  // Projects
  val testing = Seq( scalatest)
  val aggregator = Seq(
    slf4j, logback_classic, logback_core, slf4j_ext
  ) ++ testing
}

object Repositories {

}

