
import com.asidatascience.configuration.{
  DynamicConfigurationFromS3, RefreshOptions
}

import scala.util.Try
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem

import org.json4s._
import org.json4s.native.JsonMethods

import scala.concurrent.duration._

import com.amazonaws.services.s3.AmazonS3Client


case class FrozzlerConfiguration(model: String)

class WidgetFrozzler(
  configurationS3Bucket: String,
  configurationS3Key: String
) {

  implicit val actorSystem = ActorSystem()

  private def parseConfiguration(content: String) = {
    // parse the contents of the configuration file
    val contentAsJson = JsonMethods.parse(content)
    val JString(model) = (contentAsJson \ "widget-model")
    FrozzlerConfiguration(model)
  }

  val refreshOptions = RefreshOptions(
    initialDelay = 0.millis,
    updateInterval = 5.seconds
  )

  val s3Client = new AmazonS3Client()

  val configurationService = DynamicConfigurationFromS3[FrozzlerConfiguration](
    s3Client,
    configurationS3Bucket,
    configurationS3Key,
    refreshOptions
  ){ contents => Try { parseConfiguration(contents) } }

  def frozzleWidgets = {
    configurationService.currentConfiguration match {
      case Some(configuration) =>
        val currentModel = configuration.model
        println(s"Creating widget with model $currentModel")
      case None =>
        println(s"Configuration not ready")
    }
  }

  def shutdown: Future[_] = {
    configurationService.stop
    actorSystem.terminate
  }
}


object Simple extends App {
  val frozzler = new WidgetFrozzler(
    "dynamic-configuration.asidatascience.com",
    "examples/simple/sample-configuration.json"
  )

  (1 to 20) foreach { i =>
    println(s"Creating widget number $i")
    frozzler.frozzleWidgets
    Thread.sleep(1000)
  }

  Await.ready(frozzler.shutdown, 5.seconds)
}

