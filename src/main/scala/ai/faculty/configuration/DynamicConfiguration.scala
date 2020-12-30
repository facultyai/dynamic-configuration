package ai.faculty.configuration

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

import akka.actor.{ActorSystem, Cancellable}
import akka.event.Logging
import com.amazonaws.services.s3.AmazonS3
import akka.event.LogSource

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

  def fromFile[T](path: Path,
                  refreshOptions: RefreshOptions = RefreshOptions())(
      parser: String => Try[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] =
    DynamicConfiguration(refreshOptions) {
      Future {
        val file = Source.fromFile(path.toString)
        val contents = file.mkString
        file.close()
        contents
      }.flatMap { contents =>
        Future.fromTry(parser(contents))
      }
    }

  def fromS3[T](s3Client: AmazonS3,
                bucket: String,
                key: String,
                refreshOptions: RefreshOptions = RefreshOptions())(
      parser: String => Try[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] =
    DynamicConfiguration(refreshOptions) {
      Future { s3Client.getObjectAsString(bucket, key) }.flatMap { contents =>
        Future.fromTry(parser(contents))
      }
    }

}

trait DynamicConfigurationImpl[T] extends DynamicConfiguration[T] {

  def options: RefreshOptions
  def updateConfiguration: Future[T]
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext

  private implicit val logSource: LogSource[DynamicConfigurationImpl[_]] = 
    new LogSource[DynamicConfigurationImpl[_]] {
      override def genString(t: DynamicConfigurationImpl[_]): String = 
        classOf[DynamicConfigurationImpl[_]].getName()
    }

  private val log = Logging(actorSystem, this)

  override def currentConfiguration: Option[T] =
    currentConfigurationReference.get()

  private val currentConfigurationReference: AtomicReference[Option[T]] =
    new AtomicReference(None)

  var timer: Option[Cancellable] = None

  def start(): Unit = {
    val RefreshOptions(delay, interval) = options
    val task = actorSystem.scheduler.scheduleWithFixedDelay(delay, interval) { () =>
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
          log.warning("Failed to update current configuration. " +
                        "Falling back to previous version.",
                      t)
      }
    }
    timer = Some(task)
  }

  override def stop(): Unit = timer.foreach { _.cancel() }
}
