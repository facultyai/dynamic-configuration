package com.asidatascience.configuration

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.util.{Success, Try}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

abstract class BaseSpec
    extends TestKit(ActorSystem("base-spec"))
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  override implicit val patienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.millis)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  case class Configuration(timestamp: Long)

  protected val dummyConfiguration = Configuration(1L)
  protected val dummyContents = "dummy-contents"

  class TestConfigurationParser(expectedContents: String) {
    val nHits = new AtomicInteger(0)

    def parse(contents: String): Try[Configuration] = {
      contents shouldEqual expectedContents
      nHits.incrementAndGet()
      val config = dummyConfiguration
      Success(config)
    }
  }

}
