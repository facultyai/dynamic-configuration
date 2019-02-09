package ai.faculty.configuration

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

import org.scalatest.Inside
import org.scalatest.concurrent.Eventually

class DynamicConfigurationFromFileSpec
    extends BaseSpec
    with Eventually
    with Inside {

  private def withTemporaryFile(block: Path => Any): Any = {
    val file = File.createTempFile("dynamic-configuration", ".tmp")
    val path = Paths.get(file.getAbsolutePath)
    Try { block(path) }
    file.delete() shouldEqual true
  }

  private def newDynamicConfiguration(
      path: Path,
      parse: String => Try[Configuration]
  ): DynamicConfiguration[Configuration] =
    DynamicConfiguration.fromFile(
      path,
      RefreshOptions(100.millis, 300.millis)
    )(parse)

  "DynamicConfiguration from file" should "return None initially" in
    withTemporaryFile { path =>
      val parser = new TestConfigurationParser(dummyContents)
      val configuration = newDynamicConfiguration(path, parser.parse)
      configuration.currentConfiguration shouldEqual None
    }

  it should "register an initial configuration" in withTemporaryFile { path =>
    val parser = new TestConfigurationParser(dummyContents)
    val configuration = newDynamicConfiguration(path, parser.parse)

    system.scheduler.scheduleOnce(1.second) {
      val file = new PrintWriter(path.toString)
      file.write(dummyContents)
      file.close()
    }

    eventually {
      parser.nHits.get shouldEqual 1
      inside(configuration.currentConfiguration) {
        case Some(actualConfiguration) =>
          actualConfiguration shouldEqual dummyConfiguration
      }
    }
  }

}
