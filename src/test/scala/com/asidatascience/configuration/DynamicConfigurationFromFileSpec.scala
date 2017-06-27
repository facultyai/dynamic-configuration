package com.asidatascience.configuration

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

import akka.actor.ActorSystem
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Inside, Matchers}

class DynamicConfigurationFromFileSpec
extends FlatSpec
with Matchers
with BeforeAndAfterAll
with ScalaFutures
with Eventually
with Inside {

  override implicit val patienceConfig = PatienceConfig(
    timeout = 5.seconds, interval = 50.millis)

  implicit private val actorSystem = ActorSystem()

  override def afterAll(): Unit = {
    actorSystem.terminate().futureValue
  }

  final case class Configuration(timestamp: Long)

  private val dummyConfiguration = Configuration(1L)
  private val dummyContents = "dummy-contents"

  trait TestConfigurationParser {
    val nHits = new AtomicInteger(0)

    def parse(contents: String): Try[Configuration] = {
      contents shouldEqual dummyContents
      nHits.incrementAndGet()
      Success(dummyConfiguration)
    }
  }

  private def withTemporaryFile(block: Path => Unit): Unit = {
    val file = File.createTempFile("dynamic-configuration", ".tmp")
    val path = Paths.get(file.getAbsolutePath)
    try {
      block(path)
    } finally {
      file.delete()
    }
  }

  private def newDynamicConfiguration(
    path: Path, parse: String => Try[Configuration]
  ): DynamicConfiguration[Configuration] =
    DynamicConfigurationFromFile(
      path,
      RefreshOptions(100.millis, 300.millis)
    )(parse)

  "DynamicConfigurationFromFile" should "return None initially" in
  withTemporaryFile { path =>
    val parser = new TestConfigurationParser {}
    val configuration = newDynamicConfiguration(path, parser.parse)
    configuration.currentConfiguration shouldEqual None
  }

  it should "register an initial configuration" in withTemporaryFile { path =>
    val parser = new TestConfigurationParser {}
    val configuration = newDynamicConfiguration(path, parser.parse)

    actorSystem.scheduler.scheduleOnce(1.second) {
      val file = new PrintWriter(path.toString)
      file.write(dummyContents)
      file.close()
    }

    eventually {
      parser.nHits.get shouldEqual 1
      inside (configuration.currentConfiguration) {
        case Some(actualConfiguration) =>
          actualConfiguration shouldEqual dummyConfiguration
      }
    }
  }

}
