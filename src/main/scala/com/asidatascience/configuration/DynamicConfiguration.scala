package com.asidatascience.configuration

import akka.actor.ActorSystem

import scala.util.{Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.Logger

trait DynamicConfiguration[T] {
  def currentConfiguration: Option[T]
}

object DynamicConfiguration {
  def apply[T](refreshOptions: RefreshOptions)
    (updater: => Future[T])
    (implicit system: ActorSystem, context: ExecutionContext)
  :DynamicConfiguration[T] = {
    val helper = new DynamicConfigurationImpl[T] {
      override val options = refreshOptions
      override def updateConfiguration = updater
      override val actorSystem = system
      override val executionContext = context
    }
    helper.start()
    helper
  }

  def apply[T](updater: => Future[T])(implicit system: ActorSystem, context: ExecutionContext)
  : DynamicConfiguration[T] = apply(RefreshOptions())(updater)(system, context)
}


trait DynamicConfigurationImpl[T]
extends DynamicConfiguration[T] {

  def options: RefreshOptions
  def updateConfiguration: Future[T]
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  private val log = Logger(classOf[DynamicConfiguration[T]])

  override def currentConfiguration = currentConfigurationReference.get()

  private val currentConfigurationReference
  : AtomicReference[Option[T]] = new AtomicReference(None)

  def start(): Unit = {
    val RefreshOptions(delay, interval) = options
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
