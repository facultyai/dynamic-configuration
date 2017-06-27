organization := "com.asidatascience"

name := "dynamic-configuration"

scalaVersion := "2.11.12"

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.408",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % "test",
  "org.mockito" % "mockito-core" % "2.22.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-feature",
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:_",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")

publishMavenStyle := false

s3region := com.amazonaws.services.s3.model.Region.EU_Ireland

s3credentials := new com.amazonaws.auth.DefaultAWSCredentialsProviderChain()

s3acl := com.amazonaws.services.s3.model.CannedAccessControlList.Private

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
