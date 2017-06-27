package com.asidatascience.configuration

import akka.actor.{ActorSystem, Cancellable}

import scala.util.{Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.scalalogging.Logger

trait DynamicConfiguration[T] {
  def currentConfiguration: Option[T]
  def stop: Unit
}

object DynamicConfiguration {
  def apply[T](refreshOptions: RefreshOptions)
    (updater: => Future[T])
    (implicit system: ActorSystem, context: ExecutionContext)
  :DynamicConfiguration[T] = {
    val helper = new DynamicConfigurationImpl[T] {
      override def options: RefreshOptions = refreshOptions
      override def updateConfiguration: Future[T] = updater
      override def actorSystem: ActorSystem = system
      override def executionContext: ExecutionContext = context
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

  override def currentConfiguration: Option[T] = currentConfigurationReference.get()

  private val currentConfigurationReference
  : AtomicReference[Option[T]] = new AtomicReference(None)

  var timer: Option[Cancellable] = None

  def start(): Unit = {
    val RefreshOptions(delay, interval) = options
    val task = actorSystem.scheduler.schedule(delay, interval) {
      val oldConfigurationMaybe = currentConfigurationReference.get
      updateConfiguration.onComplete {
        case Success(newConfiguration)
            if oldConfigurationMaybe.contains(newConfiguration) =>
        case Success(newConfiguration) =>
          currentConfigurationReference.compareAndSet(
            oldConfigurationMaybe, Some(newConfiguration))
        case Failure(t) =>
          log.warn(
            "Failed to update current configuration. " +
            "Falling back to previous version.", t)
      }
    }
    timer = Some(task)
  }

  override def stop(): Unit = { timer.foreach { _.cancel } }
}
