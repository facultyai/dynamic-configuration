name := "dynamic-configuration-simple-example"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.10",
  "com.amazonaws" % "aws-java-sdk" % "1.11.66",
  "ai.faculty" %% "dynamic-configuration" % "0.4.0-SNAPSHOT",
  "org.json4s" %% "json4s-native" % "3.6.10"
)

scalacOptions ++= Seq("-feature", "-deprecation")
