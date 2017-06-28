package com.asidatascience.configuration

import scala.concurrent.{Future, ExecutionContext}

import scala.util.Try

import akka.actor.ActorSystem

import com.amazonaws.services.s3.AmazonS3

object DynamicConfigurationFromS3 {
  def apply[T](
    s3Client: AmazonS3,
    bucket: String,
    key: String,
    refreshOptions: RefreshOptions = RefreshOptions()
  )(parser: String => Try[T])
  (implicit system: ActorSystem, context: ExecutionContext): DynamicConfiguration[T] = {

    DynamicConfiguration(refreshOptions) {
      Future {
        val contents: String = s3Client.getObjectAsString(bucket, key)
        val configurationTry = parser(contents)
        configurationTry.get
      }
    }
  }

}
