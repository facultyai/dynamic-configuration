sonatypeProfileName := "ai.faculty"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/facultyai/dynamic-configuration"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/facultyai/dynamic-configuration"),
    "scm:git@github.com:facultyai/dynamic-configuration.git"
  )
)
developers := List(
  Developer(id="pbugnion", name="Pascal Bugnion", email="pascal@bugnion.org", url=url("https://pascalbugnion.net")),
  Developer(id="srstevenson", name="Scott Stevenson", email="scott@stevenson.io", url=url("https://scott.stevenson.io")),
  Developer(id="tomasmilata", name="Tomáš Milata", email="tomas.milata@gmail.com", url=url("https://github.com/tomas-milata"))
)
