name := "aggregator"

version := "1.0"

scalaVersion := "2.11.7"


lazy val aggregator = (project in file(".")).
  settings(
    name := "aggregator",
    version := "0.1-SNAPSHOT",
    //    javaOptions +="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
    libraryDependencies ++= Dependencies.aggregator
  )
