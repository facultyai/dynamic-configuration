name := "dynamic-configuration-simple-example"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.amazonaws" % "aws-java-sdk" % "1.11.66",
  "com.asidatascience" %% "dynamic-configuration" % "562f9b7d70a5205a65622519849281f519b54748-SNAPSHOT",
  "org.json4s" %% "json4s-native" % "3.5.0"
)

scalacOptions ++= Seq("-feature", "-deprecation", "-Ywarn-unused-import")
