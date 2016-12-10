package com.asidatascience.configuration

import akka.actor.ActorSystem

import scala.util.{Success, Failure}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.Logger

trait DynamicConfigurationService[T] {
  def currentConfiguration: Option[T]
}

object DynamicConfigurationService {
  def apply[T](
    initialDelay: FiniteDuration,
    updateInterval: FiniteDuration)
  (updater: => Future[T])
  (implicit system: ActorSystem, context: ExecutionContext):
DynamicConfigurationService[T] = {
    val helper = new DynamicConfigurationServiceImpl[T] {
      override val delay = initialDelay
      override val interval = updateInterval
      override def updateConfiguration = updater
      override val actorSystem = system
      override val executionContext = context
    }
    helper.start()
    helper
  }
}


trait DynamicConfigurationServiceImpl[T]
extends DynamicConfigurationService[T] {

  def delay: FiniteDuration
  def interval: FiniteDuration
  def updateConfiguration: Future[T]
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  private val log = Logger(classOf[DynamicConfigurationService[T]])

  override def currentConfiguration = currentConfigurationReference.get()

  private val currentConfigurationReference
  : AtomicReference[Option[T]] = new AtomicReference(None)

  def start(): Unit = {
    actorSystem.scheduler.schedule(delay, interval) {
      val oldConfigurationMaybe = currentConfigurationReference.get
      updateConfiguration.onComplete {
        case Success(newConfiguration)
            if Some(newConfiguration) == oldConfigurationMaybe =>
        case Success(newConfiguration) =>
          val changed =
            currentConfigurationReference.compareAndSet(
              oldConfigurationMaybe, Some(newConfiguration))
        case Failure(t) =>
          log.warn(
            "Failed to update current configuration. " +
            "Falling back to previous version.", t)
      }
    }
  }
}
