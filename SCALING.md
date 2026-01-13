# Scaling the Consultant System

## Architectural Approach

The system is designed for **horizontal scaling** using stateless application instances.

## Implemented Scaling Components

### 1. **Caching - Redis**

- **RedisCacheService** - CacheService implementation via Redis
- Reduces database load
- LRU eviction policy
- TTL for automatic cleanup

**Usage:**

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

- **CircuitBreakerService** - protection against cascading failures
- 3 states: Closed → Open → HalfOpen
- Automatic recovery

**Usage:**

```scala
val circuitBreaker = new CircuitBreakerService(maxFailures = 5, resetTimeout = 30.seconds)
circuitBreaker.protect(externalApiCall)
```

### 3. **Metrics and Monitoring**

- **MetricsCollector** - performance metrics collection
- Request metrics: total, success rate, avg response time
- Service metrics: active connections, cache hit rate
- Endpoints: `/metrics` for Prometheus

### 4. **Database Connection Pooling**

- **DatabasePoolConfig** - HikariCP optimization
- Settings for master (write) and read replica
- Connection leak detection
- Optimal pool sizing

### 5. **Containerization**

- **Dockerfile** - multi-stage build
- JVM optimized for containers
- Health checks
- Non-root user for security

### 6. **Local Cluster - Docker Compose**

- 3 application instances
- PostgreSQL master
- Redis cache
- Nginx load balancer (least connections)
- Rate limiting and caching at nginx level

### 7. **Kubernetes/AWS ECS**

- **deployment.yaml** - production-ready configuration
- Horizontal Pod Autoscaler (3-10 pods)
- Anti-affinity for node distribution
- Rolling updates with zero downtime
- Liveness/Readiness probes

## Scaling Strategy

### Tier 1: Up to 1,000 RPS

```
- 3 application instances
- 1 PostgreSQL master
- 1 Redis instance
- Local load balancer (nginx)
```

### Tier 2: Up to 10,000 RPS

```
- 5-10 application instances (HPA)
- PostgreSQL master + 2 read replicas
- Redis Cluster (3 masters, 3 replicas)
- AWS ALB/NLB
- CloudFront CDN
```

### Tier 3: Up to 100,000+ RPS

```
- Auto-scaling up to 50+ pods
- PostgreSQL sharding + pooling (PgBouncer)
- Redis Cluster with partitioning
- Async processing via SQS
- ElastiCache Redis
- RDS Multi-AZ + Read Replicas
- Global load balancing
```

## AWS Deployment Architecture

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
│ S3           │ (Files/documents)
└──────────────┘
```

## Running Local Cluster

### Docker Compose

```bash
# Build image
docker build -t consultant-api:latest .

# Start cluster
docker-compose up -d

# Verify
curl http://localhost/health
curl http://localhost/api/users

# View logs
docker-compose logs -f app-1

# Scale
docker-compose up -d --scale app=5
```

### Kubernetes

```bash
# Create secrets
kubectl create secret generic db-credentials \
  --from-literal=host=<RDS_ENDPOINT> \
  --from-literal=username=consultant_user \
  --from-literal=password=<PASSWORD>

# Deploy
kubectl apply -f kubernetes/deployment.yaml

# Check autoscaling
kubectl get hpa
kubectl top pods
```

## Monitoring and Alerts

### Metrics to Track

1. **Request rate** - requests per second
2. **Error rate** - percentage of errors
3. **Response time (p50, p95, p99)** - latency
4. **Database connection pool** - active/idle connections
5. **Cache hit rate** - cache effectiveness
6. **JVM heap usage** - memory utilization
7. **CPU/Memory utilization** - resource load

### Monitoring Endpoints

- `/health` - health check
- `/metrics` - Prometheus metrics
- CloudWatch for AWS
- Grafana dashboards

## Best Practices

1. ✅ **Stateless application** - sessions in Redis, not in memory
2. ✅ **Connection pooling** - efficient DB connection usage
3. ✅ **Caching** - Redis for hot data
4. ✅ **Circuit breakers** - overload protection
5. ✅ **Rate limiting** - DDoS protection
6. ✅ **Async processing** - SQS for heavy operations
7. ✅ **Graceful shutdown** - proper request completion
8. ✅ **Health checks** - automatic problem detection
9. ✅ **Horizontal scaling** - add instances, don't increase size

## Future Improvements

- [ ] Database sharding for very large volumes
- [ ] CQRS (Command Query Responsibility Segregation)
- [ ] Event sourcing for audit trail
- [ ] GraphQL federation for microservices
- [ ] Service mesh (Istio) for advanced networking
- [ ] Distributed tracing (Jaeger/Zipkin)
