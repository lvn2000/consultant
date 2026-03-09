/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
