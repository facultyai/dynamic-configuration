package com.asidatascience.configuration

import scala.concurrent.duration._

case class RefreshOptions(
  initialDelay: FiniteDuration = 0.millis,
  updateInterval: FiniteDuration = 5.minutes
)
