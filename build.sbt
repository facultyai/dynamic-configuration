
name := "dynamic-configuration"

scalaVersion := "2.11.8"

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

git.useGitDescribe := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.amazonaws" % "aws-java-sdk" % "1.11.66",
  "org.mockito" % "mockito-core" % "2.2.29",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

scalacOptions ++= Seq("-feature", "-deprecation", "-Ywarn-unused-import")

publishMavenStyle := false

s3region := com.amazonaws.services.s3.model.Region.EU_Ireland

s3credentials := new com.amazonaws.auth.EnvironmentVariableCredentialsProvider()

s3acl := com.amazonaws.services.s3.model.CannedAccessControlList.Private

publishTo := {
  val prefix = if (isSnapshot.value) "snapshots" else "releases"
  Some(s3resolver.value("ASI "+prefix+" S3 bucket", s3(s"asi-$prefix-repository")) withIvyPatterns)
}
