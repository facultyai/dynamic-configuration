package com.asidatascience.configuration

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Success, Try}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server

class DynamicConfigurationFromHttpSpec
extends TestKit(ActorSystem("dynamic-configuration-from-http-spec"))
with FlatSpecLike
with Matchers
with Eventually
with Inside
with BeforeAndAfterAll
with ScalaFutures {

  override implicit val patienceConfig = PatienceConfig(
    timeout = 1.seconds, interval = 50.millis)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  case class Configuration(timestamp: Long)

  val dummyConfiguration = Configuration(1L)
  val dummyContents = "dummy-contents"

  trait TestConfigurationParser {
    val nHits = new AtomicInteger(0)

    def parse(contents: String): Try[Configuration] = {
      contents shouldEqual dummyContents
      nHits.incrementAndGet()
      val config = dummyConfiguration
      Success(config)
    }
  }

  def withDynamicConfiguration(
    parser: TestConfigurationParser)(
    block: DynamicConfiguration[Configuration] => Any): Unit = {
    Server.withRouter() {
      case GET(p"/dummy-url") =>
        parser.nHits.incrementAndGet()
        Action { Results.Ok(dummyContents) }
    } { implicit port =>
      WsTestClient.withClient { testClient =>
        Thread.sleep(1000) // scalastyle:ignore magic.number
        val dynamicConfiguration = DynamicConfigurationFromHttp(
          testClient, "/dummy-url", RefreshOptions(100.millis, 300.millis)
        )(parser.parse)
        block(dynamicConfiguration)
        testClient.close()
      }
    }
  }

  "DynamicConfigurationFromHttp" should "return None initially" in {
    val parser = new TestConfigurationParser {}
    withDynamicConfiguration(parser) { configuration =>
      configuration.currentConfiguration shouldEqual None
    }
  }

  it should "register an initial configuration" in {
    val parser = new TestConfigurationParser {}
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
