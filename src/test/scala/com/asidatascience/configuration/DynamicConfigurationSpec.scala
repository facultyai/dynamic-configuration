package com.asidatascience.configuration

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

import org.scalatest._
import org.scalatest.concurrent._

class DynamicConfigurationSpec extends BaseSpec with Eventually with Inside {

  case object IntentionalException extends Exception("intentional exception")

  private def newConfiguration = Configuration(System.nanoTime)

  trait TestConfigurationUpdater {
    val nHits = new AtomicInteger(0)
    var lastConfigurationUpdate: Option[Configuration] = None
    def update: Future[Configuration] =
      Future {
        Thread.sleep(500) // scalastyle:ignore magic.number
        nHits.incrementAndGet()
        val config = newConfiguration
        lastConfigurationUpdate = Some(config)
        config
      }
  }

  private def newDynamicConfiguration(
      updater: => Future[Configuration]
  ): DynamicConfiguration[Configuration] =
    DynamicConfiguration[Configuration](
      RefreshOptions(500.millis, 1.seconds)
    )(updater)

  "DynamicConfiguration" should "return None initially" in {
    val updater = new TestConfigurationUpdater {}
    val configuration = newDynamicConfiguration(updater.update)

    configuration.currentConfiguration shouldEqual None
  }

  it should "register an initial configuration" in {
    val updater = new TestConfigurationUpdater {}
    val configuration = newDynamicConfiguration(updater.update)

    eventually {
      updater.nHits.get shouldEqual 1
      inside(configuration.currentConfiguration) {
        case Some(actualConfiguration) =>
          actualConfiguration shouldEqual updater.lastConfigurationUpdate.get
      }
    }
  }

  it should "update the configuration regularly" in {
    val updater = new TestConfigurationUpdater {}
    val configuration = newDynamicConfiguration(updater.update)

    eventually {
      updater.nHits.get shouldEqual 2
      configuration.currentConfiguration should matchPattern {
        case Some(Configuration(_)) =>
      }
    }
  }

  it should "return the old configuration if updating fails" in {
    val firstConfiguration = newConfiguration
    val secondConfiguration = newConfiguration

    val updater = new TestConfigurationUpdater {
      override def update: Future[Configuration] =
        Future {
          Thread.sleep(500) // scalastyle:ignore magic.number
          nHits.incrementAndGet()
        }.flatMap { nHits =>
          if (nHits == 1) {
            Future.successful(firstConfiguration)
          } else if (nHits > 1 && nHits < 5) {
            Future.failed(IntentionalException)
          } else {
            Future.successful(secondConfiguration)
          }
        }
    }

    val configuration = newDynamicConfiguration(updater.update)

    eventually {
      updater.nHits.get shouldEqual 1
      inside(configuration.currentConfiguration) {
        case Some(config) => config shouldEqual firstConfiguration
      }
    }

    eventually {
      updater.nHits.get should (be > 1 and be < 5)
      inside(configuration.currentConfiguration) {
        case Some(config) => config shouldEqual firstConfiguration
      }
    }

    eventually {
      updater.nHits.get should be > 5
      inside(configuration.currentConfiguration) {
        case Some(config) => config shouldEqual secondConfiguration
      }
    }

  }

}
