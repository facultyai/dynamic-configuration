
# Dynamic configuration tools

This repository provides tools for setting up configuration that refreshes at
particular intervals. It assumes that the current configuration lives in a file
on Amazon S3. It tries to refresh the configuration at regular intervals.

Assume that, for instance, you want to create a class to frozzle some widgets.
This class needs access to the current model of the widget to be frozzled. To
avoid hard-coding the model in your code, you decide to keep a reference to the
model in a file on S3. You want to be able to update that file and have your
widget frozzler automatically pick up the changes.

Let's assume that your configuration is formatted as JSON:

```json
{
  "widget-model": "ASI-1292"
}
```

To load and automatically refresh the configuration from S3, create a case class
that represents your configuration (e.g. `FrozzlerConfiguration` in the example
below). Then, call `DynamicConfigurationFromS3`, passing in the bucket and key
at which your configuration file is located and a method for converting from the
string content of your configuration to a `Try[FrozzlerConfiguration]`.

`DynamicConfigurationFromS3` will return a `DynamicConfiguration` object with a `currentConfiguration` method. This returns an option with either the current configuration, or `None` if the configuration is not loaded yet.

```scala

import com.asidatascience.configuration.{DynamicConfigurationFromS3, RefreshOptions}

import scala.util.Try
import org.json4s._

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
}
```

This is turned into a fully functional example in the `/examples/simple` directory.
