package com.asidatascience.configuration

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import akka.actor.ActorSystem
import play.api.libs.ws.WSClient

final case class HttpException(msg: String) extends Exception(msg)

object DynamicConfigurationFromHttp {
  def apply[T](
      wsClient: WSClient,
      url: String,
      refreshOptions: RefreshOptions = RefreshOptions()
  )(parser: String => Try[T])(
      implicit system: ActorSystem,
      context: ExecutionContext): DynamicConfiguration[T] =
    DynamicConfiguration(refreshOptions) {
      val responseFuture = wsClient
        .url(url)
        .get

      responseFuture.flatMap { response =>
        if (response.status == 200) {
          Future.fromTry(parser(response.body))
        } else {
          Future.failed(
            HttpException(
              s"Request failed with status code ${response.status}"))
        }
      }
    }

}
