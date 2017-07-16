package com.asidatascience.configuration

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

import akka.actor.ActorSystem

object DynamicConfigurationFromFile {
  def apply[T](
      path: Path,
      refreshOptions: RefreshOptions = RefreshOptions()
  )(parser: String => Try[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] =
    DynamicConfiguration(refreshOptions) {
      Future {
        val file = Source.fromFile(path.toString)
        val contents = file.mkString
        file.close()
        contents
      }.flatMap { contents =>
        Future.fromTry { parser(contents) }
      }
    }

}
