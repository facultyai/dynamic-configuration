package com.asidatascience.configuration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

import com.amazonaws.services.s3.AmazonS3
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent._
import org.scalatest.mockito.MockitoSugar

class DynamicConfigurationFromS3Spec
    extends BaseSpec
    with Eventually
    with Inside
    with MockitoSugar {

  private val dummyBucket = "test-bucket"
  private val dummyKey = "test-key"

  private val mockS3Client = mock[AmazonS3]
  when(mockS3Client.getObjectAsString(dummyBucket, dummyKey))
    .thenReturn(dummyContents)

  def newDynamicConfiguration(
      parse: String => Try[Configuration]
  ): DynamicConfiguration[Configuration] =
    DynamicConfiguration.fromS3(
      mockS3Client,
      dummyBucket,
      dummyKey,
      RefreshOptions(100.millis, 1000.millis)
    )(parse)

  "DynamicConfiguration from S3" should "return None initially" in {
    val parser = new TestConfigurationParser(dummyContents)
    val configuration = newDynamicConfiguration(parser.parse)
    configuration.currentConfiguration shouldEqual None
  }

  it should "register an initial configuration" in {
    val parser = new TestConfigurationParser(dummyContents)
    val configuration =
      newDynamicConfiguration(parser.parse)

    eventually {
      parser.nHits.get shouldEqual 1
      inside(configuration.currentConfiguration) {
        case Some(actualConfiguration) =>
          actualConfiguration shouldEqual dummyConfiguration
      }
    }
  }

}
