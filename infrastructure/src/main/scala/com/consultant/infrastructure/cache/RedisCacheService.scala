package com.consultant.infrastructure.cache

import cats.effect.IO
import cats.syntax.all.*
import com.consultant.core.ports.CacheService

// Redis implementation stub - TODO: integrate redis4cats
// For now, returns empty cache (noop implementation)
class RedisCacheService extends CacheService:

  override def get(key: String): IO[Option[String]] =
    IO.pure(None) // TODO: redis.get(key)

  override def set(key: String, value: String, ttlSeconds: Int): IO[Unit] =
    IO.unit // TODO: redis.setEx(key, value, ttlSeconds)

  override def delete(key: String): IO[Unit] =
    IO.unit // TODO: redis.del(key)
