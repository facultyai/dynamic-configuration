package com.asidatascience.configuration

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import akka.actor.{ActorSystem, Cancellable}
import play.api.Logger

trait DynamicConfiguration[T] {
  def currentConfiguration: Option[T]
  def stop(): Unit
}

object DynamicConfiguration {
  def apply[T](refreshOptions: RefreshOptions)(updater: => Future[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] = {
    val helper = new DynamicConfigurationImpl[T] {
      override def options: RefreshOptions = refreshOptions
      override def updateConfiguration: Future[T] = updater
      override def actorSystem: ActorSystem = system
      override def executionContext: ExecutionContext = context
    }
    helper.start()
    helper
  }

  def apply[T](updater: => Future[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] =
    apply(RefreshOptions())(updater)(system, context)
}

trait DynamicConfigurationImpl[T] extends DynamicConfiguration[T] {

  def options: RefreshOptions
  def updateConfiguration: Future[T]
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  private val log = Logger(classOf[DynamicConfiguration[T]])

  override def currentConfiguration: Option[T] =
    currentConfigurationReference.get()

  private val currentConfigurationReference: AtomicReference[Option[T]] =
    new AtomicReference(None)

  var timer: Option[Cancellable] = None

  def start(): Unit = {
    val RefreshOptions(delay, interval) = options
    val task = actorSystem.scheduler.schedule(delay, interval) {
      val oldConfigurationMaybe = currentConfigurationReference.get
      updateConfiguration.onComplete {
        case Success(newConfiguration)
            if oldConfigurationMaybe.contains(newConfiguration) =>
          log.debug("Dynamic configuration unchanged from current version.")
        case Success(newConfiguration) =>
          currentConfigurationReference.compareAndSet(oldConfigurationMaybe,
                                                      Some(newConfiguration))
          log.info("Dynamic configuration updated with new version.")
        case Failure(t) =>
          log.warn("Failed to update dynamic configuration. " +
                     "Falling back to previous version.",
                   t)
      }
    }
    timer = Some(task)
  }

  override def stop(): Unit = timer.foreach { _.cancel }
}
