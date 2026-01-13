# Компоненты масштабирования - Quick Start

## 🎯 Что добавлено

### 1. Redis Кеш (RedisCacheService)

**Файл:** `infrastructure/cache/RedisCacheService.scala`
**Цель:** Снижение нагрузки на БД через кеширование hot data

### 2. Circuit Breaker (CircuitBreakerService)

**Файл:** `infrastructure/resilience/CircuitBreakerService.scala`
**Цель:** Защита от каскадных сбоев при перегрузке внешних сервисов

### 3. Метрики (MetricsCollector)

**Файл:** `infrastructure/metrics/MetricsCollector.scala`
**Цель:** Мониторинг производительности (RPS, latency, cache hit rate)

### 4. Database Pool Config

**Файл:** `data/config/DatabasePoolConfig.scala`
**Цель:** Оптимизация HikariCP для высоких нагрузок + read replicas

### 5. Docker + Kubernetes

**Файлы:** `Dockerfile`, `docker-compose.yml`, `kubernetes/deployment.yaml`
**Цель:** Контейнеризация + оркестрация для horizontal scaling

### 6. Load Balancing

**Файл:** `nginx.conf`
**Цель:** Nginx с least connections, rate limiting, кешированием

### 7. Health & Metrics API

**Файлы:** `api/routes/HealthRoutes.scala`, `api/routes/MetricsRoutes.scala`
**Цель:** Endpoints для Kubernetes health checks и Prometheus

## 🚀 Быстрый запуск локального кластера

```bash
# 1. Собрать образ
docker build -t consultant-api:latest .

# 2. Запустить кластер (3 инстанса + PostgreSQL + Redis + nginx)
docker-compose up -d

# 3. Проверить
curl http://localhost/health
curl http://localhost/metrics

# 4. Тестовый запрос
curl http://localhost/api/users

# 5. Просмотр логов
docker-compose logs -f app-1

# 6. Масштабирование до 5 инстансов
docker-compose up -d --scale app-1=2 --scale app-2=2 --scale app-3=1

# 7. Остановка
docker-compose down
```

## 📊 Стратегия масштабирования

| Нагрузка | Конфигурация |
|----------|--------------|
| **< 1K RPS** | 3 инстанса, 1 DB, 1 Redis, Local LB |
| **< 10K RPS** | 5-10 pods (HPA), Read Replicas, Redis Cluster, AWS ALB |
| **< 100K RPS** | 50+ pods, DB Sharding, ElastiCache, CloudFront CDN |

## 🔧 Следующие шаги

1. **Добавить Redis в AppConfig:**

```scala
case class RedisConfig(
  host: String,
  port: Int
)
```

1. **Интегрировать в Server.scala:**

```scala
redis <- Redis[IO].utf8("redis://localhost:6379").toResource
cacheService = new RedisCacheService(redis)
cachedSpecialistService = new CachedSpecialistService(specialistRepo, cacheService)
```

1. **Добавить метрики middleware:**

```scala
metricsCollector = new MetricsCollector()
wrappedRoutes = routes.map(r => metricsCollector.recordRequest(r))
```

1. **Deploy в AWS:**
   - ECS/EKS для приложения
   - RDS PostgreSQL (Multi-AZ + Read Replicas)
   - ElastiCache Redis
   - ALB для load balancing

## 📈 Мониторинг

- **Health:** `GET /health` - Kubernetes liveness/readiness
- **Metrics:** `GET /metrics` - Prometheus endpoint
- **Swagger:** `GET /docs` - API документация

Подробности в [SCALING.md](./SCALING.md)
