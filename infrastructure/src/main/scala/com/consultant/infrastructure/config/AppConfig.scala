package com.consultant.infrastructure.config

import cats.effect.IO
import cats.syntax.all.*
import ciris.*
import scala.concurrent.duration.*

case class AppConfig(
  server: ServerConfig,
  database: DatabaseConfig,
  aws: Option[AwsConfig],
  storage: StorageConfig
)

case class ServerConfig(
  host: String,
  port: Int
)

case class DatabaseConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  poolSize: Int
)

case class AwsConfig(
  region: String,
  s3BucketName: String,
  sqsQueuePrefix: String,
  senderEmail: String
)

case class StorageConfig(
  useAws: Boolean,
  localBasePath: Option[String]
)

object AppConfig:

  def load: IO[AppConfig] =
    (
      env("SERVER_HOST").as[String].default("0.0.0.0"),
      env("SERVER_PORT").as[Int].default(8080),
      env("DB_DRIVER").as[String].default("org.postgresql.Driver"),
      env("DB_URL").as[String].default("jdbc:postgresql://localhost:5432/consultant"),
      env("DB_USER").as[String].default("postgres"),
      env("DB_PASSWORD").as[String].default("postgres"),
      env("DB_POOL_SIZE").as[Int].default(32),
      env("USE_AWS").as[Boolean].default(false),
      env("AWS_REGION").as[String].option,
      env("AWS_S3_BUCKET").as[String].option,
      env("AWS_SQS_QUEUE_PREFIX").as[String].option,
      env("AWS_SENDER_EMAIL").as[String].option,
      env("LOCAL_STORAGE_PATH").as[String].option
    ).parMapN {
      (host, port, driver, url, user, pass, poolSize, useAws, region, bucket, queuePrefix, email, localPath) =>
        val awsConfig =
          if useAws then
            Some(
              AwsConfig(
                region.getOrElse("us-east-1"),
                bucket.getOrElse("consultant-files"),
                queuePrefix.getOrElse("consultant"),
                email.getOrElse("[email protected]")
              )
            )
          else None

        AppConfig(
          ServerConfig(host, port),
          DatabaseConfig(driver, url, user, pass, poolSize),
          awsConfig,
          StorageConfig(useAws, localPath.orElse(Some("./storage")))
        )
    }.load[IO]
