package com.consultant.api.di

import cats.effect.IO
import doobie.util.transactor.Transactor
import com.consultant.infrastructure.config.AppConfig

/**
 * AppModule is the root dependency injection container.
 *
 * This object wires together all modules and provides the complete dependency graph. It's used by the Server to access
 * all required dependencies.
 *
 * Example usage: {{ val module = new AppModule { override val xa = transactor override val config = appConfig } }}
 */
trait AppModule extends RoutesModule:
  val xa: Transactor[IO]
  val config: AppConfig
end AppModule

/** Factory for creating AppModule instances with runtime dependencies. */
object AppModule:
  def apply(transactor: Transactor[IO], appConfig: AppConfig): AppModule =
    new AppModule:
      val xa     = transactor
      val config = appConfig
end AppModule
