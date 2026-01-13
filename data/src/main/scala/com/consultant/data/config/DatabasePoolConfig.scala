package com.consultant.data.config

import cats.effect.{ IO, Resource }
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

/** Оптимизированная конфигурация пула подключений для высоких нагрузок */
object DatabasePoolConfig:

  case class PoolSettings(
    maximumPoolSize: Int = 20,           // Максимум подключений
    minimumIdle: Int = 5,                // Минимум idle подключений
    connectionTimeout: Long = 30000,     // 30 секунд на установку соединения
    idleTimeout: Long = 600000,          // 10 минут idle
    maxLifetime: Long = 1800000,         // 30 минут максимальная жизнь подключения
    leakDetectionThreshold: Long = 60000 // 1 минута для детекции утечек
  )

  def createTransactor(
    driver: String,
    url: String,
    user: String,
    password: String,
    poolSettings: PoolSettings = PoolSettings()
  ): Resource[IO, HikariTransactor[IO]] =
    for
      // Execution context для блокирующих операций БД
      connectEC <- ExecutionContexts.fixedThreadPool[IO](poolSettings.maximumPoolSize)

      // Создаем HikariCP transactor с оптимизированными настройками
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = driver,
        url = url,
        user = user,
        pass = password,
        connectEC = connectEC
      )

      // Настраиваем HikariCP пул
      _ <- Resource.eval(
        xa.configure { ds =>
          IO {
            ds.setMaximumPoolSize(poolSettings.maximumPoolSize)
            ds.setMinimumIdle(poolSettings.minimumIdle)
            ds.setConnectionTimeout(poolSettings.connectionTimeout)
            ds.setIdleTimeout(poolSettings.idleTimeout)
            ds.setMaxLifetime(poolSettings.maxLifetime)
            ds.setLeakDetectionThreshold(poolSettings.leakDetectionThreshold)

            // Дополнительные оптимизации
            ds.setConnectionTestQuery("SELECT 1")
            ds.setAutoCommit(false)
            ds.setPoolName("consultant-pool")
          }
        }
      )
    yield xa

  /** Конфигурация для Read Replica (только чтение) */
  def createReadReplicaTransactor(
    driver: String,
    url: String,
    user: String,
    password: String
  ): Resource[IO, HikariTransactor[IO]] =
    val readPoolSettings = PoolSettings(
      maximumPoolSize = 30, // Больше подключений для чтения
      minimumIdle = 10,
      connectionTimeout = 20000
    )
    createTransactor(driver, url, user, password, readPoolSettings)
