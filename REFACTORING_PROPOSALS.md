# Code Refactoring Proposals

## Executive Summary

This document presents a comprehensive analysis of the Consultant Backend codebase with specific refactoring proposals organized by priority, impact, and effort required. The codebase follows a clean hexagonal architecture with Scala 3, Cats Effect, Http4s, and Tapir.

---

## 1. High Priority - Critical Improvements

### 1.1 Error Handling Standardization

**Current State:**
- Inconsistent error handling across services
- Some services use `.handleError` while others don't
- Error mapping in `DtoMappers.toErrorResponse` is a partial function (missing cases)
- Database error messages are parsed as strings

**Issues Found:**
```scala
// SpecialistService.scala - String-based error parsing
.handleError { error =>
  val errorMsg = error.getMessage
  if errorMsg != null && errorMsg.contains("duplicate key value violates unique constraint") 
  ...
}
```

**Proposed Changes:**
1. **Create a centralized error handler** in `core` module:
```scala
// core/src/main/scala/com/consultant/core/error/ErrorHandler.scala
object ErrorHandler:
  def handleRepositoryError[A](io: IO[A]): IO[Either[DomainError, A]] = 
    io.map(Right(_)).handleError {
      case ex: org.postgresql.util.PSQLException => 
        parsePostgresError(ex).asLeft
      case ex => 
        DomainError.UnexpectedError(ex.getMessage).asLeft
    }
  
  private def parsePostgresError(ex: org.postgresql.util.PSQLException): DomainError = 
    ex.getSQLState match
      case "23505" => // unique_violation
        parseUniqueViolation(ex.getMessage)
      case "23503" => // foreign_key_violation
        DomainError.ReferencedRecordNotFound(ex.getMessage)
      case _ => DomainError.DatabaseError(ex.getMessage)
```

2. **Extend DomainError enum** with missing cases:
```scala
enum DomainError:
  // ... existing cases ...
  case ReferencedRecordNotFound(message: String)
  case UnexpectedError(message: String)
  case Unauthorized
  case RateLimitExceeded
```

3. **Complete the error mapper** in DtoMappers:
```scala
def toErrorResponse(error: DomainError): ErrorResponse = error match
  // ... existing cases ...
  case DomainError.ReferencedRecordNotFound(msg) => 
    ErrorResponse("CONFLICT", msg)
  case DomainError.UnexpectedError(msg) => 
    ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")
```

**Impact:** High - Improves reliability and debugging
**Effort:** Medium
**Files to Modify:**
- `core/src/main/scala/com/consultant/core/domain/DomainError.scala`
- `api/src/main/scala/com/consultant/api/DtoMappers.scala`
- All service implementations

---

### 1.2 Dependency Injection Container

**Current State:**
- Manual dependency wiring in `Server.scala` (lines 247-288)
- 14 repositories and 7 services created manually
- Difficult to test and maintain

**Proposed Changes:**
1. **Create a Module pattern** using Scala's self-types:
```scala
// infrastructure/src/main/scala/com/consultant/infrastructure/di/Modules.scala
trait RepositoryModule:
  def xa: Transactor[IO]
  
  lazy val userRepo: UserRepository = PostgresUserRepository(xa)
  lazy val specialistRepo: SpecialistRepository = PostgresSpecialistRepository(xa, connectionRepo)
  // ... etc

trait ServiceModule extends RepositoryModule:
  lazy val userService: UserService = UserService(userRepo, sessionRepo, Some(notificationPreferenceRepo))
  lazy val specialistService: SpecialistService = SpecialistService(specialistRepo, categoryRepo)
  // ... etc

object AppModule extends ServiceModule:
  val xa: Transactor[IO] = ??? // provided at runtime
```

2. **Alternative: Use a lightweight DI library** like `distage` or `izumi`:
```scala
// Define bindings
val bindings = new ModuleDef {
  make[Transactor[IO]].fromResource(DatabaseConfig.makeTransactor[IO])
  make[UserRepository].from[PostgresUserRepository]
  make[UserService]
}
```

**Impact:** High - Improves testability and maintainability
**Effort:** Medium-High
**Files to Modify:**
- `api/src/main/scala/com/consultant/api/Server.scala`
- New: `infrastructure/src/main/scala/com/consultant/infrastructure/di/`

---

### 1.3 Database Transaction Management

**Current State:**
- No explicit transaction management across multiple repositories
- Each operation runs in its own transaction
- Risk of inconsistent state

**Proposed Changes:**
1. **Create a Unit of Work pattern**:
```scala
// core/src/main/scala/com/consultant/core/ports/UnitOfWork.scala
trait UnitOfWork:
  def transaction[A](io: ConnectionIO[A]): IO[A]
  
  def transact[A](operations: List[ConnectionIO[A]]): IO[List[A]]

// Usage in services
def createConsultationWithNotification(request: CreateConsultationRequest): IO[Either[DomainError, Consultation]] =
  unitOfWork.transaction {
    for
      consultation <- consultationRepo.create(request, price).to[ConnectionIO]
      _ <- notificationRepo.create(notification).to[ConnectionIO]
    yield consultation
  }
```

2. **Add transaction boundaries** to service methods that need atomicity

**Impact:** High - Prevents data inconsistency
**Effort:** Medium
**Files to Modify:**
- `core/src/main/scala/com/consultant/core/ports/` (new UnitOfWork trait)
- `data/src/main/scala/com/consultant/data/` (implementation)
- Service layer methods

---

## 2. Medium Priority - Architecture Improvements

### 2.1 Validation Layer Extraction

**Current State:**
- Validation logic scattered in services
- Duplicated validation patterns
- No centralized validation framework

**Examples:**
```scala
// SpecialistService.scala
if request.categoryRates.exists(_.hourlyRate <= 0) then
  val invalidRate = request.categoryRates.find(_.hourlyRate <= 0).map(_.hourlyRate).getOrElse(BigDecimal(0))
  IO.pure(Left(DomainError.InvalidPrice(invalidRate)))
```

**Proposed Changes:**
1. **Create a Validation DSL**:
```scala
// core/src/main/scala/com/consultant/core/validation/Validator.scala
object Validator:
  def validate[A](value: A): ValidationResult[A] = ???
  
  extension [A](value: A)
    def must(predicate: A => Boolean, error: => DomainError): ValidationResult[A]
    def mustNot(predicate: A => Boolean, error: => DomainError): ValidationResult[A]

// Usage
def validateCreateRequest(req: CreateSpecialistRequest): Either[DomainError, Unit] =
  validate(req)
    .field(_.email, _.must(isValidEmail, DomainError.InvalidEmail(_.email)))
    .field(_.categoryRates, _.forall(_.hourlyRate > 0), DomainError.InvalidPrice(0))
    .result
```

2. **Extract validators per domain**:
```scala
// core/src/main/scala/com/consultant/core/validation/SpecialistValidator.scala
object SpecialistValidator:
  def validate(request: CreateSpecialistRequest): Either[DomainError, CreateSpecialistRequest]
  def validateUpdate(specialist: Specialist): Either[DomainError, Specialist]
```

**Impact:** Medium - Cleaner code, reusable validation
**Effort:** Medium
**Files to Modify:**
- New validation package in `core`
- All service implementations

---

### 2.2 DTO Simplification and Mapping

**Current State:**
- `DtoMappers.scala` is growing (186 lines) and will become unwieldy
- Manual mapping between domain and DTOs
- Risk of inconsistency

**Proposed Changes:**
1. **Use automatic derivation** with Tapir's generic derivation:
```scala
// Define mappers as type classes
trait DtoMapper[Domain, Dto]:
  def toDto(domain: Domain): Dto
  def toDomain(dto: Dto): Domain

// Derive automatically where possible
given DtoMapper[User, UserDto] = Derivation.deriveMapper
```

2. **Split mappers by domain**:
```
api/src/main/scala/com/consultant/api/mappers/
  UserMappers.scala
  SpecialistMappers.scala
  ConsultationMappers.scala
  package.scala // exports all
```

3. **Use Chimney** for boilerplate-free mapping:
```scala
import io.scalaland.chimney.dsl._

def toUserDto(user: User): UserDto = 
  user.into[UserDto].transform
```

**Impact:** Medium - Reduces boilerplate, improves maintainability
**Effort:** Low-Medium
**Files to Modify:**
- Split `DtoMappers.scala` into domain-specific files
- Add Chimney dependency

---

### 2.3 Route Organization and Composition

**Current State:**
- Routes are well-organized but have some inconsistencies
- Some endpoints lack proper error variants
- SwaggerUI disabled due to assembly issues

**Proposed Changes:**
1. **Standardize endpoint definitions**:
```scala
// Create a base endpoint builder
object ApiEndpoints:
  def baseEndpoint(name: String, description: String) = endpoint
    .in("api")
    .name(name)
    .description(description)
    .errorOut(
      oneOf[ErrorResponse](
        oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(StatusCode.Conflict).and(jsonBody[ErrorResponse])),
        oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorResponse]))
      )
    )
```

2. **Group routes by version**:
```
routes/
  v1/
    UserRoutes.scala
    SpecialistRoutes.scala
  v2/ // Future API versions
```

3. **Fix SwaggerUI** by using a separate docs module or fixing the assembly merge strategy

**Impact:** Medium - Better API documentation, consistency
**Effort:** Medium
**Files to Modify:**
- `api/src/main/scala/com/consultant/api/routes/` (restructure)
- `build.sbt` (assembly settings)

---

### 2.4 Configuration Management Enhancement

**Current State:**
- Using Ciris for configuration
- Hardcoded defaults in `Server.scala`
- No environment-specific configs

**Proposed Changes:**
1. **Create environment-specific configs**:
```scala
// infrastructure/src/main/scala/com/consultant/infrastructure/config/Configs.scala
object Configs:
  def load(environment: Environment): IO[AppConfig] = environment match
    case Environment.Development => loadDevelopmentConfig
    case Environment.Staging => loadStagingConfig
    case Environment.Production => loadProductionConfig
```

2. **Use HOCON for complex configurations**:
```hocon
# application.conf
app {
  database {
    pool-size = 10
    pool-size = ${?DB_POOL_SIZE}
    connection-timeout = 30s
  }
  
  features {
    notifications = true
    caching = false
  }
}
```

3. **Add configuration validation**:
```scala
def validateConfig(config: AppConfig): Either[ConfigError, AppConfig]
```

**Impact:** Medium - Better deployment flexibility
**Effort:** Low
**Files to Modify:**
- `infrastructure/src/main/scala/com/consultant/infrastructure/config/`

---

## 3. Low Priority - Code Quality Improvements

### 3.1 Test Coverage Expansion

**Current State:**
- Test dependencies present (ScalaTest, ScalaMock)
- Limited test files visible

**Proposed Changes:**
1. **Add property-based testing** with ScalaCheck:
```scala
// Example property test
property("User creation should validate email format") {
  forAll(invalidEmails) { email =>
    val request = CreateUserRequest("login", email, "Name", None, UserRole.Client, None, Set.empty)
    userService.createUser(request).map(_.isLeft) shouldBe true
  }
}
```

2. **Add integration tests** with TestContainers:
```scala
class PostgresRepositorySpec extends CatsEffectSuite with TestContainerForAll:
  override val containerDef: ContainerDef = PostgreSQLContainer()
  // Test repository implementations against real Postgres
```

3. **Add contract tests** for API endpoints

**Impact:** High - Prevents regressions
**Effort:** High
**Files to Modify:**
- Add test directories in each module

---

### 3.2 Logging and Observability

**Current State:**
- Basic logging with Logback
- MetricsCollector exists but minimal usage
- No distributed tracing

**Proposed Changes:**
1. **Add structured logging**:
```scala
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

def createUser(request: CreateUserRequest)(using Logger[IO]): IO[Either[DomainError, User]] =
  info"Creating user with email: ${request.email}" >>
  // ... logic ...
  result.flatTap {
    case Right(user) => info"User created successfully: ${user.id}"
    case Left(error) => warn"Failed to create user: $error"
  }
```

2. **Add OpenTelemetry integration**:
```scala
// Automatic tracing for all service calls
trait TracedService[F[_]]:
  def trace[A](name: String)(fa: F[A]): F[A]
```

3. **Add health check endpoints** for each dependency:
```scala
// Check database, external services, etc.
GET /health/deep - Full system health check
```

**Impact:** Medium - Better debugging and monitoring
**Effort:** Medium
**Files to Modify:**
- Add logging to all services
- Extend health check routes

---

### 3.3 Code Style and Consistency

**Current State:**
- Some inconsistencies in naming and style
- Scalafmt configured but could be stricter

**Proposed Changes:**
1. **Enforce stricter Scalafmt rules**:
```
# .scalafmt.conf
maxColumn = 100
trailingCommas = always
rewrite.rules = [RedundantBraces, RedundantParens, SortModifiers]
```

2. **Add WartRemover** for common Scala pitfalls:
```scala
// build.sbt
wartremoverWarnings ++= Warts.allBut(Wart.DefaultArguments, Wart.ImplicitParameter)
```

3. **Standardize naming conventions**:
   - Use `*Repository` for all repos (consistent)
   - Use `*Service` for business logic
   - Use `*Routes` for HTTP handlers
   - Use `*Dto` for data transfer objects

**Impact:** Low - Code consistency
**Effort:** Low
**Files to Modify:**
- `.scalafmt.conf`
- `build.sbt`
- Various source files for naming

---

### 3.4 Documentation Improvements

**Current State:**
- Good documentation in `docs/` folder
- Inline code documentation minimal

**Proposed Changes:**
1. **Add Scaladoc** to all public APIs:
```scala
/** Service for managing user accounts and authentication.
  * 
  * @param userRepo Repository for user data access
  * @param sessionRepo Repository for session management
  */
class UserService(
  userRepo: UserRepository,
  sessionRepo: SessionRepository,
  ...
)
```

2. **Add architecture decision records (ADRs)**:
```
docs/adr/
  001-use-tapir-for-api-definition.md
  002-postgres-over-dynamodb.md
  003-hexagonal-architecture.md
```

**Impact:** Low - Better onboarding
**Effort:** Low
**Files to Modify:**
- All public classes and methods

---

## 4. Performance Optimizations

### 4.1 Database Query Optimization

**Current State:**
- Doobie queries use default settings
- No query result caching
- N+1 query risk in some operations

**Proposed Changes:**
1. **Add query logging and analysis**:
```scala
// Log slow queries
implicit val logHandler: LogHandler = LogHandler {
  case Success(sql, args, exec, processing) =>
    if exec > 100.millis then
      logger.warn(s"Slow query (${exec}ms): $sql")
}
```

2. **Implement result caching** for frequently accessed data:
```scala
class CachedCategoryRepository(
  underlying: CategoryRepository,
  cache: CacheService
) extends CategoryRepository:
  def listAll(): IO[List[Category]] =
    cache.getOrUpdate("categories.all", 5.minutes)(underlying.listAll())
```

3. **Add pagination** to all list endpoints with cursor-based pagination

**Impact:** High - Better performance at scale
**Effort:** Medium
**Files to Modify:**
- Repository implementations
- Add caching layer

---

### 4.2 Async Processing

**Current State:**
- Email notifications sent synchronously
- No background job processing

**Proposed Changes:**
1. **Implement event-driven architecture**:
```scala
// core/src/main/scala/com/consultant/core/events/EventBus.scala
trait EventBus[F[_]]:
  def publish[A: Encoder](event: A): F[Unit]
  def subscribe[A: Decoder](handler: A => F[Unit]): F[Unit]

// Usage
def createConsultation(request: CreateConsultationRequest) =
  for
    consultation <- consultationRepo.create(request, price)
    _ <- eventBus.publish(ConsultationCreatedEvent(consultation.id))
  yield consultation
```

2. **Add SQS integration** for background jobs:
```scala
// Process notifications asynchronously
notificationService.sendEmail(...).start // Fire and forget with error handling
```

**Impact:** Medium - Better response times
**Effort:** High
**Files to Modify:**
- Add event system
- Modify service implementations

---

## Implementation Roadmap

### Phase 1: Critical Fixes (Week 1-2)
1. Complete error handling in `DtoMappers`
2. Add missing `DomainError` cases
3. Extract validation layer
4. Add transaction management

### Phase 2: Architecture Improvements (Week 3-4)
1. Implement dependency injection container
2. Refactor DTO mappers
3. Standardize route definitions
4. Enhance configuration management

### Phase 3: Quality & Performance (Week 5-6)
1. Add comprehensive tests
2. Implement caching layer
3. Add structured logging
4. Optimize database queries

### Phase 4: Polish (Week 7+)
1. Add documentation
2. Code style enforcement
3. Performance monitoring
4. Load testing

---

## Summary Table

| Proposal | Priority | Impact | Effort | Files Modified |
|----------|----------|--------|--------|----------------|
| Error Handling Standardization | High | High | Medium | 5-10 |
| Dependency Injection | High | High | Medium | 3-5 |
| Transaction Management | High | High | Medium | 5-10 |
| Validation Layer | Medium | Medium | Medium | 10-15 |
| DTO Simplification | Medium | Medium | Low | 3-5 |
| Route Organization | Medium | Medium | Medium | 10-15 |
| Configuration Enhancement | Medium | Medium | Low | 2-3 |
| Test Coverage | Medium | High | High | 15-20 |
| Logging/Observability | Low | Medium | Medium | 10-15 |
| Code Style | Low | Low | Low | 20+ |
| Documentation | Low | Low | Low | All |
| Query Optimization | Medium | High | Medium | 5-10 |
| Async Processing | Low | Medium | High | 10-15 |

---

## Appendix: Code Metrics

- **Total Scala Files:** ~80
- **Lines of Code:** ~8,000-10,000 (estimated)
- **Modules:** 4 (core, data, infrastructure, api)
- **Frontend Apps:** 3 (admin-app, client-app, specialist-app)
- **Test Coverage:** Unknown (needs assessment)

---

*Generated based on codebase analysis as of March 2026*
