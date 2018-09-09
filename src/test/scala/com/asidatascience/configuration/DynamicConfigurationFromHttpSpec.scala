package com.asidatascience.configuration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import org.scalatest.Inside
import org.scalatest.concurrent.Eventually
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server

class DynamicConfigurationFromHttpSpec
    extends BaseSpec
    with Eventually
    with Inside {

  override implicit val patienceConfig =
    PatienceConfig(timeout = 2.seconds, interval = 50.millis)

  private def withDynamicConfiguration(parser: TestConfigurationParser)(
      block: DynamicConfiguration[Configuration] => Any): Unit =
    Server.withRouter() {
      case GET(p"/dummy-url") =>
        parser.nHits.incrementAndGet()
        Action { Results.Ok(dummyContents) }
    } { implicit port =>
      WsTestClient.withClient { testClient =>
        Thread.sleep(1000) // scalastyle:ignore magic.number
        val dynamicConfiguration = DynamicConfigurationFromHttp(
          testClient,
          "/dummy-url",
          RefreshOptions(100.millis, 300.millis)
        )(parser.parse)
        block(dynamicConfiguration)
        testClient.close()
      }
    }

  "DynamicConfigurationFromHttp" should "return None initially" in {
    val parser = new TestConfigurationParser(dummyContents)
    withDynamicConfiguration(parser) { configuration =>
      configuration.currentConfiguration shouldEqual None
    }
  }

  it should "register an initial configuration" in {
    val parser = new TestConfigurationParser(dummyContents)
    withDynamicConfiguration(parser) { configuration =>
      eventually {
        parser.nHits.get should be > 0
        inside(configuration.currentConfiguration) {
          case Some(actualConfiguration) =>
            actualConfiguration shouldEqual dummyConfiguration
        }
      }
    }
  }

}
