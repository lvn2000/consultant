# Масштабирование системы Consultant

## Архитектурный подход

Система спроектирована для **горизонтального масштабирования** (horizontal scaling) с использованием stateless инстансов приложения.

## Реализованные компоненты масштабирования

### 1. **Кеширование - Redis**

- **RedisCacheService** - реализация CacheService через Redis
- Уменьшает нагрузку на БД
- LRU eviction политика
- TTL для автоочистки

**Применение:**

```scala
cacheService.get(s"specialist:$id").flatMap {
  case Some(cached) => IO.pure(decode[Specialist](cached))
  case None => 
    specialistRepository.findById(id).flatTap { specialist =>
      cacheService.set(s"specialist:$id", specialist.asJson.noSpaces, Some(5.minutes))
    }
}
```

### 2. **Circuit Breaker**

- **CircuitBreakerService** - защита от каскадных сбоев
- 3 состояния: Closed → Open → HalfOpen
- Автоматическое восстановление

**Применение:**

```scala
val circuitBreaker = new CircuitBreakerService(maxFailures = 5, resetTimeout = 30.seconds)
circuitBreaker.protect(externalApiCall)
```

### 3. **Метрики и мониторинг**

- **MetricsCollector** - сбор метрик производительности
- Request metrics: total, success rate, avg response time
- Service metrics: active connections, cache hit rate
- Endpoints: `/metrics` для Prometheus

### 4. **Database Connection Pooling**

- **DatabasePoolConfig** - HikariCP оптимизация
- Настройки для master (write) и read replica
- Connection leak detection
- Optimal pool sizing

### 5. **Контейнеризация**

- **Dockerfile** - multi-stage build
- Оптимизированный JVM для контейнеров
- Health checks
- Non-root пользователь для безопасности

### 6. **Локальный кластер - Docker Compose**

- 3 инстанса приложения
- PostgreSQL master
- Redis cache
- Nginx load balancer (least connections)
- Rate limiting и кеширование на уровне nginx

### 7. **Kubernetes/AWS ECS**

- **deployment.yaml** - production-ready конфигурация
- Horizontal Pod Autoscaler (3-10 pods)
- Anti-affinity для распределения по нодам
- Rolling updates без downtime
- Liveness/Readiness probes

## Стратегия масштабирования

### Tier 1: До 1000 RPS

```
- 3 инстанса приложения
- 1 PostgreSQL master
- 1 Redis instance
- Local load balancer (nginx)
```

### Tier 2: До 10,000 RPS

```
- 5-10 инстансов приложения (HPA)
- PostgreSQL master + 2 read replicas
- Redis Cluster (3 masters, 3 replicas)
- AWS ALB/NLB
- CloudFront CDN
```

### Tier 3: До 100,000+ RPS

```
- Auto-scaling до 50+ pods
- PostgreSQL sharding + pooling (PgBouncer)
- Redis Cluster с партиционированием
- Асинхронная обработка через SQS
- ElastiCache Redis
- RDS Multi-AZ + Read Replicas
- Global load balancing
```

## AWS Deployment архитектура

```
┌─────────────────────────────────────────────┐
│ Route 53 (DNS)                              │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│ CloudFront CDN                              │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│ Application Load Balancer (ALB)            │
│ - Health checks                             │
│ - SSL termination                           │
│ - Connection draining                       │
└────────────────┬────────────────────────────┘
                 │
      ┌──────────┼──────────┐
      │          │          │
┌─────▼───┐ ┌───▼────┐ ┌──▼─────┐
│ ECS/EKS │ │ ECS/EKS│ │ ECS/EKS│ (Auto Scaling)
│ Task 1  │ │ Task 2 │ │ Task 3 │
└─────┬───┘ └───┬────┘ └──┬─────┘
      │         │         │
      └─────────┼─────────┘
                │
      ┌─────────┴─────────┐
      │                   │
┌─────▼────────┐   ┌─────▼──────────┐
│ RDS          │   │ ElastiCache    │
│ PostgreSQL   │   │ Redis          │
│ - Master     │   │ - Cluster mode │
│ - Replicas   │   └────────────────┘
└──────────────┘
      │
┌─────▼────────┐
│ S3           │ (Файлы/документы)
└──────────────┘
```

## Запуск локального кластера

### Docker Compose

```bash
# Сборка образа
docker build -t consultant-api:latest .

# Запуск кластера
docker-compose up -d

# Проверка
curl http://localhost/health
curl http://localhost/api/users

# Просмотр логов
docker-compose logs -f app-1

# Масштабирование
docker-compose up -d --scale app=5
```

### Kubernetes

```bash
# Создание секретов
kubectl create secret generic db-credentials \
  --from-literal=host=<RDS_ENDPOINT> \
  --from-literal=username=consultant_user \
  --from-literal=password=<PASSWORD>

# Deploy
kubectl apply -f kubernetes/deployment.yaml

# Проверка автомасштабирования
kubectl get hpa
kubectl top pods
```

## Мониторинг и алерты

### Метрики для отслеживания

1. **Request rate** - запросов в секунду
2. **Error rate** - процент ошибок
3. **Response time (p50, p95, p99)** - латентность
4. **Database connection pool** - активные/idle подключения
5. **Cache hit rate** - эффективность кеша
6. **JVM heap usage** - использование памяти
7. **CPU/Memory utilization** - нагрузка на ресурсы

### Endpoints для мониторинга

- `/health` - health check
- `/metrics` - Prometheus metrics
- CloudWatch для AWS
- Grafana dashboards

## Best Practices

1. ✅ **Stateless приложение** - сессии в Redis, не в памяти
2. ✅ **Connection pooling** - эффективное использование БД подключений
3. ✅ **Caching** - Redis для hot data
4. ✅ **Circuit breakers** - защита от перегрузки
5. ✅ **Rate limiting** - защита от DDoS
6. ✅ **Async processing** - SQS для тяжелых операций
7. ✅ **Graceful shutdown** - корректное завершение запросов
8. ✅ **Health checks** - автоматическое обнаружение проблем
9. ✅ **Horizontal scaling** - добавление инстансов, не увеличение размера

## Дальнейшие улучшения

- [ ] Database sharding для очень больших объемов
- [ ] CQRS (Command Query Responsibility Segregation)
- [ ] Event sourcing для audit trail
- [ ] GraphQL federation для микросервисов
- [ ] Service mesh (Istio) для advanced networking
- [ ] Distributed tracing (Jaeger/Zipkin)
