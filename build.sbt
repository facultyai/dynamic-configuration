organization := "ai.faculty"

name := "dynamic-configuration"

version := "0.4.0-SNAPSHOT"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.10",
  // Not used but needs to be pinned for compatibility. See
  //  https://doc.akka.io/docs/akka/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed
  "com.typesafe.akka" %% "akka-stream" % "2.6.10",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.408",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.10" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.9" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-feature",
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:_",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-value-discard"
)

publishMavenStyle := true

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
   else
    Opts.resolver.sonatypeStaging
  )

scalafmtTestOnCompile in Compile := true
scalafmtTestOnCompile in Test := true

scalafmtFailTest in Compile := false
scalafmtFailTest in Test := false

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
  .in(Compile)
  .toTask("")
  .value

(compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value

lazy val testCompileScalastyle = taskKey[Unit]("testCompileScalastyle")

testCompileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
  .in(Test)
  .toTask("")
  .value

(compile in Test) := ((compile in Test) dependsOn testCompileScalastyle).value
