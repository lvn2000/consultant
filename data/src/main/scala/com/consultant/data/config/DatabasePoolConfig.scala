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
package com.consultant.data.config

import cats.effect.{ IO, Resource }
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

/** Optimized connection pool configuration for high loads */
object DatabasePoolConfig:

  case class PoolSettings(
    maximumPoolSize: Int = 20,           // Maximum connections
    minimumIdle: Int = 5,                // Minimum idle connections
    connectionTimeout: Long = 30000,     // 30 seconds for connection establishment
    idleTimeout: Long = 600000,          // 10 minutes idle
    maxLifetime: Long = 1800000,         // 30 minutes maximum connection lifetime
    leakDetectionThreshold: Long = 60000 // 1 minute for leak detection
  )

  def createTransactor(
    driver: String,
    url: String,
    user: String,
    password: String,
    poolSettings: PoolSettings = PoolSettings()
  ): Resource[IO, HikariTransactor[IO]] =
    for
      // Execution context for blocking DB operations
      connectEC <- ExecutionContexts.fixedThreadPool[IO](poolSettings.maximumPoolSize)

      // Create HikariCP transactor with optimized settings
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = driver,
        url = url,
        user = user,
        pass = password,
        connectEC = connectEC
      )

      // Configure HikariCP pool
      _ <- Resource.eval(
        xa.configure { ds =>
          IO {
            ds.setMaximumPoolSize(poolSettings.maximumPoolSize)
            ds.setMinimumIdle(poolSettings.minimumIdle)
            ds.setConnectionTimeout(poolSettings.connectionTimeout)
            ds.setIdleTimeout(poolSettings.idleTimeout)
            ds.setMaxLifetime(poolSettings.maxLifetime)
            ds.setLeakDetectionThreshold(poolSettings.leakDetectionThreshold)

            // Additional optimizations
            ds.setConnectionTestQuery("SELECT 1")
            ds.setAutoCommit(false)
            ds.setPoolName("consultant-pool")
          }
        }
      )
    yield xa

  /** Configuration for Read Replica (read-only) */
  def createReadReplicaTransactor(
    driver: String,
    url: String,
    user: String,
    password: String
  ): Resource[IO, HikariTransactor[IO]] =
    val readPoolSettings = PoolSettings(
      maximumPoolSize = 30, // More connections for reading
      minimumIdle = 10,
      connectionTimeout = 20000
    )
    createTransactor(driver, url, user, password, readPoolSettings)
