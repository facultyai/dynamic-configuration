package com.asidatascience.configuration

import org.scalatest._
import org.scalatest.concurrent._
import org.scalatest.mockito.MockitoSugar

import org.mockito.Mockito.when

import scala.util.{Try, Success}

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.amazonaws.services.s3.AmazonS3

import java.util.concurrent.atomic.AtomicInteger

class DynamicConfigurationFromS3Spec
extends FlatSpec
with Matchers
with BeforeAndAfterAll
with Eventually
with Inside
with MockitoSugar
with ScalaFutures {

  override implicit val patienceConfig = PatienceConfig(
    timeout = 5.seconds, interval = 50.millis)

  implicit val actorSystem = ActorSystem()

  override def afterAll {
    actorSystem.terminate().futureValue
    ()
  }

  case class Configuration(timestamp: Long)

  val dummyConfiguration = Configuration(1L)

  val dummyS3Contents = "dummy-contents"
  val dummyBucket = "test-bucket"
  val dummyKey = "test-key"

  val mockS3Client = mock[AmazonS3]
  when(mockS3Client.getObjectAsString(dummyBucket, dummyKey))
    .thenReturn(dummyS3Contents)

  trait TestConfigurationParser {
    val nHits = new AtomicInteger(0)

    def parse(contents: String): Try[Configuration] = {
      contents shouldEqual dummyS3Contents
      nHits.incrementAndGet()
      val config = dummyConfiguration
      Success(config)
    }
  }

  def newDynamicConfiguration(
    parse: String => Try[Configuration]
  ): DynamicConfiguration[Configuration] =
    DynamicConfigurationFromS3(
      mockS3Client,
      dummyBucket,
      dummyKey,
      RefreshOptions(100.millis, 300.millis)
    )(parse)

  "DynamicConfigurationFromS3" should "return None initially" in {
    val parser = new TestConfigurationParser {}
    val configuration = newDynamicConfiguration(parser.parse)
    configuration.currentConfiguration shouldEqual None
  }

  it should "register an initial configuration" in {
    val parser = new TestConfigurationParser {}
    val configuration =
      newDynamicConfiguration(parser.parse)

    eventually {
      parser.nHits.get shouldEqual 1
      inside (configuration.currentConfiguration) {
        case Some(actualConfiguration) =>
          actualConfiguration shouldEqual dummyConfiguration
      }
    }
  }

}
