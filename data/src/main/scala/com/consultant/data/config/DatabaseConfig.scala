package com.consultant.data.config

import cats.effect._
import doobie._
import doobie.hikari._

case class DbConfig(
  driver: String,
  url: String,
  user: String,
  password: String
)

object DatabaseConfig {

  def makeTransactor[F[_]: Async](config: DbConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        config.driver,
        config.url,
        config.user,
        config.password,
        ce
      )
    } yield xa
}
