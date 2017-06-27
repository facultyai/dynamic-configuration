sonatypeProfileName := "com.asidatascience"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/ASIDataScience/dynamic-configuration"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/ASIDataScience/dynamic-configuration"),
    "scm:git@github.com:ASIDataScience/dynamic-configuration.git"
  )
)
developers := List(
  Developer(id="pbugnion", name="Pascal Bugnion", email="pascal@bugnion.org", url=url("https://pascalbugnion.net")),
  Developer(id="srstevenson", name="Scott Stevenson", email="scott@stevenson.io", url=url("https://scott.stevenson.io"))
)
