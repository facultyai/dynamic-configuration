
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
  "widget-model": "WM-2512"
}
```

```scala

import com.asidatascience.configuration.{DynamicConfigurationFromS3, RefreshOptions}

import scala.util.Try
import org.json4s._

import scala.concurrent.duration._

import com.amazonaws.services.s3.AmazonS3Client

case class FrozzlerConfiguration(model: String)

class WidgetFrozzler {

  private def parseConfiguration(content: String) = {
    // parse the contents of the configuration file
    val contentAsJson = parse(content)
    val JString(model) = (contentAsJson \ "widget-model")
    FrozzlerConfiguration(model)
  }
  
  val refreshOptions = RefreshOptions(
    initialDelay = 0.millis,
    fetchInterval = 5.minutes
  )
  
  val s3Client = new AmazonS3Client()

  val configurationService = DynamicConfigurationFromS3[FrozzlerConfiguration](
    s3Client,
    "widget-frozzling-bucket",
    "/path/to/configuration-file.json",
    refreshOptions
  ){ contents => Try { parseConfiguration(contents) } }
  
  
  def frozzleWidgets = {
    val currentModel = configurationService.currentConfiguration.model
    println(s"Creating widget with model $currentModel")
  }
}
```
