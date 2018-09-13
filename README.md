# Dynamic configuration tools

[![Build Status](https://travis-ci.org/ASIDataScience/dynamic-configuration.svg)](https://travis-ci.org/ASIDataScience/dynamic-configuration)

This repository provides tools for setting up configuration that refreshes at
particular intervals. It assumes that the current configuration lives in a file
on the local file system, on Amazon S3, or on a web server that speaks HTTP. It
tries to refresh the configuration at regular intervals.

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

To load and automatically refresh the configuration from S3, create a case
class that represents your configuration (e.g. `FrozzlerConfiguration` in the
example below). Then, call `DynamicConfiguration.fromS3`, passing in the bucket
and key at which your configuration file is located and a method for converting
from the string content of your configuration to a
`Try[FrozzlerConfiguration]`.

`DynamicConfiguration.fromS3` will return a `DynamicConfiguration` object with
a `currentConfiguration` method. This returns an option with either the current
configuration, or `None` if the configuration is not loaded yet.

```scala
import scala.concurrent.duration._
import scala.util.Try

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.asidatascience.configuration.{DynamicConfiguration, RefreshOptions}
import org.json4s._

final case class FrozzlerConfiguration(model: String)

class WidgetFrozzler(
    configurationS3Bucket: String,
    configurationS3Key: String
) {

  implicit val actorSystem = ActorSystem()

  private def parseConfiguration(content: String) = {
    // Parse the contents of the configuration file.
    val contentAsJson = JsonMethods.parse(content)
    val JString(model) = (contentAsJson \ "widget-model")
    FrozzlerConfiguration(model)
  }

  val refreshOptions = RefreshOptions(
    initialDelay = 0.millis,
    updateInterval = 5.seconds
  )

  val s3Client = AmazonS3ClientBuilder
    .standard()
    .withRegion(Regions.EU_WEST_1)
    .build

  lazy val configurationService =
    DynamicConfiguration.fromS3[FrozzlerConfiguration](
      s3Client,
      configurationS3Bucket,
      configurationS3Key,
      refreshOptions
    ) { contents =>
      Try { parseConfiguration(contents) }
    }

  def frozzleWidgets =
    configurationService.currentConfiguration match {
      case Some(configuration) =>
        val currentModel = configuration.model
        println(s"Creating widget with model $currentModel")
      case None =>
        println("Configuration not ready")
    }
}
```

This is turned into a fully functional example in the
[`/examples/simple`][example] directory.

[example]: https://github.com/ASIDataScience/dynamic-configuration/tree/master/examples/simple

## About ASI Data Science

[![ASI Data Science](https://cloud.githubusercontent.com/assets/5845679/19309499/140ca760-907d-11e6-9234-4601a6a516ca.png)][ASI Data Science]

dynamic-configuration is maintained by [ASI Data Science]. We empower
organisations to become more data-driven by providing first class software,
skills, and advisory solutions.

[ASI Data Science]: https://www.asidatascience.com/
