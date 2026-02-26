# Consultant Backend - Architecture Diagrams

This document contains comprehensive Mermaid diagrams for the Scala-based consultant backend system.

## 1. Project Structure - Subprojects Overview

```mermaid
flowchart TD
    subgraph Root["Consultant Backend (Root Project)"]
        direction TB
        
        subgraph Core["core - Domain & Business Logic"]
            C1[Domain Models<br/>User, Specialist<br/>Consultation, Category]
            C2[Business Services<br/>UserService<br/>SpecialistService<br/>ConsultationService]
            C3[Port Interfaces<br/>RepositoryPorts<br/>InfrastructurePorts<br/>SecurityPorts]
        end
        
        subgraph Data["data - PostgreSQL Layer"]
            D1[Doobie Repositories<br/>PostgresUserRepository<br/>PostgresSpecialistRepository]
            D2[Database Config<br/>Connection Pool<br/>Flyway Migrations]
        end
        
        subgraph Infra["infrastructure - External Services"]
            I1[AWS Adapters<br/>S3, SNS, SQS, SES]
            I2[Local Adapters<br/>Mock Services<br/>LocalStorage]
            I3[Security<br/>JWT, OIDC, Password<br/>AuthenticationService]
            I4[Infrastructure<br/>Redis Cache<br/>Metrics, CircuitBreaker]
        end
        
        subgraph API["api - HTTP API Layer"]
            A1[Tapir Routes<br/>UserRoutes, AuthRoutes<br/>SpecialistRoutes]
            A2[DTOs & Mappers<br/>Request/Response<br/>Data Transformation]
            A3[Middleware<br/>Authentication<br/>Token Validation]
            A4[Server<br/>Http4s Ember<br/>Swagger UI]
        end
    end
    
    Root -->|"aggregates"| Core
    Root -->|"aggregates"| Data
    Root -->|"aggregates"| Infra
    Root -->|"aggregates"| API
    
    Data -->|"dependsOn"| Core
    Infra -->|"dependsOn"| Core
    API -->|"dependsOn"| Core & Data & Infra
    
    style Root fill:#f9f9f9,stroke:#333,stroke-width:2px
    style Core fill:#e1f5fe,stroke:#0288d1,stroke-width:2px
    style Data fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    style Infra fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    style API fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
```

---

## 2. Hexagonal Architecture Overview

```mermaid
flowchart TB
    subgraph Drivers["Driving Side (Primary)"]
        direction TB
        HTTP[HTTP Requests<br/>REST API]
        DTO[DTOs & Validators]
        AUTH[Authentication<br/>Middleware]
    end
    
    subgraph Hexagon["Hexagon (Core Domain)"]
        direction TB
        ROUTES[API Routes<br/>Tapir Endpoints]
        SERVICES[Domain Services<br/>Business Logic]
        DOMAIN[Domain Models<br/>User, Specialist<br/>Consultation]
        PORTS[Port Interfaces<br/>RepositoryPorts<br/>InfrastructurePorts]
    end
    
    subgraph Driven["Driven Side (Secondary)"]
        direction TB
        subgraph DB["Database Adapters"]
            PG[(PostgreSQL<br/>Doobie Repositories)]
            FW[Flyway Migrations]
        end
        
        subgraph EXT["External Adapters"]
            AWS[AWS Services<br/>S3, SNS, SQS, SES<br/>via LocalStack]
            JWT[JWT Token Service<br/>OIDC Verifier]
            LOCAL[Local Storage<br/>Mock Notifications]
        end
    end
    
    HTTP --> AUTH
    AUTH --> DTO
    DTO --> ROUTES
    ROUTES --> SERVICES
    SERVICES --> DOMAIN
    DOMAIN --> PORTS
    
    PORTS --> PG
    PORTS --> AWS
    PORTS --> JWT
    PORTS --> LOCAL
    
    FW --> PG
    
    style Hexagon fill:#e1f5fe,stroke:#0288d1,stroke-width:3px
    style Drivers fill:#f5f5f5,stroke:#666,stroke-width:1px
    style Driven fill:#fff3e0,stroke:#f57c00,stroke-width:1px
```

---

## 3. Sequence Diagram - User Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer<br/>(AuthenticationMiddleware)
    participant Auth as AuthService<br/>(infrastructure)
    participant JWT as JwtTokenService<br/>(infrastructure)
    participant Repo as UserRepository<br/>(data)
    participant DB[(PostgreSQL)]
    participant Pass as PasswordHashingService

    Client->>API: POST /api/auth/login<br/>{login, password}
    API->>Auth: authenticate(login, password)
    Auth->>Repo: findByLoginOrEmail(login)
    Repo->>DB: SELECT * FROM users WHERE login=?
    DB-->>Repo: User record
    Repo-->>Auth: User entity
    Auth->>Pass: verify(password, hash)
    Pass-->>Auth: Boolean result
    
    alt Invalid Credentials
        Auth-->>API: Left(AuthError)
        API-->>Client: 401 Unauthorized
    else Valid Credentials
        Auth->>JWT: generateToken(User)
        JWT-->>Auth: AccessToken + RefreshToken
        Auth->>Repo: saveRefreshToken(token)
        Repo->>DB: INSERT INTO refresh_tokens
        DB-->>Repo: Success
        Repo-->>Auth: Token saved
        Auth-->>API: Right(AuthResponse)
        API-->>Client: 200 OK<br/>{accessToken, refreshToken, user}
    end
```

---

## 4. Sequence Diagram - Search Specialists

```mermaid
sequenceDiagram
    participant Client
    participant API as SpecialistRoutes<br/>(api)
    participant Service as SpecialistService<br/>(core)
    participant SpecRepo as SpecialistRepository<br/>(data)
    participant CatRepo as CategoryRepository<br/>(data)
    participant DB[(PostgreSQL)]

    Client->>API: GET /api/specialists/search?<br/>categoryId=&minRating=
    API->>Service: searchSpecialists(criteria)
    
    Service->>CatRepo: exists(categoryId)
    CatRepo->>DB: SELECT EXISTS...
    DB-->>CatRepo: Boolean
    CatRepo-->>Service: Category exists
    
    Service->>SpecRepo: searchByCriteria(criteria)
    SpecRepo->>DB: SELECT specialists JOIN<br/>specialist_category_rates<br/>WHERE category_id=?<br/>AND rating>=?
    DB-->>SpecRepo: List[Specialist]
    SpecRepo-->>Service: Specialists
    
    Service-->>API: List[Specialist]
    API-->>Client: 200 OK<br/>[SpecialistDto, ...]
```

---

## 5. Sequence Diagram - Create Consultation

```mermaid
sequenceDiagram
    participant Client
    participant API as ConsultationRoutes
    participant Service as ConsultationService<br/>(core)
    participant SpecRepo as SpecialistRepository
    participant UserRepo as UserRepository
    participant ConsultRepo as ConsultationRepository
    participant Notif as NotificationService<br/>(infrastructure)
    participant DB[(PostgreSQL)]
    participant Queue as SQS Queue<br/>(AWS/Local)

    Client->>API: POST /api/consultations<br/>{userId, specialistId, ...}
    API->>Service: createConsultation(request)
    
    Service->>SpecRepo: findById(specialistId)
    SpecRepo->>DB: SELECT * FROM specialists
    DB-->>SpecRepo: Specialist
    SpecRepo-->>Service: Specialist
    
    Service->>UserRepo: findById(userId)
    UserRepo->>DB: SELECT * FROM users
    DB-->>UserRepo: User
    UserRepo-->>Service: User
    
    alt Specialist Available
        Service->>ConsultRepo: save(consultation)
        ConsultRepo->>DB: INSERT INTO consultations
        DB-->>ConsultRepo: ConsultationId
        ConsultRepo-->>Service: Consultation
        
        Service->>Notif: sendNotification<br/>specialist, "New Request"
        Notif->>Queue: sendMessage SNS/SQS
        Queue-->>Notif: MessageId
        Notif-->>Service: NotificationSent
        
        Service-->>API: Right(Consultation)
        API-->>Client: 201 Created<br/>{consultation}
    else Specialist Unavailable
        Service-->>API: Left(SpecialistNotAvailable)
        API-->>Client: 400 Bad Request
    end
```

---

## 6. Class Diagram - Domain Models

```mermaid
classDiagram
    class User {
        +UserId: UUID
        +login: String
        +email: String
        +name: String
        +phone: Option[String]
        +role: UserRole
        +countryId: Option[CountryId]
        +languages: Set[LanguageId]
        +createdAt: Instant
        +updatedAt: Instant
    }
    
    class Specialist {
        +SpecialistId: UUID
        +email: String
        +name: String
        +phone: String
        +bio: String
        +categoryRates: List[SpecialistCategoryRate]
        +isAvailable: Boolean
        +connections: List[SpecialistConnection]
        +countryId: Option[CountryId]
        +languages: Set[LanguageId]
        +createdAt: Instant
        +updatedAt: Instant
    }
    
    class SpecialistCategoryRate {
        +categoryId: CategoryId
        +hourlyRate: BigDecimal
        +experienceYears: Int
        +rating: Option[BigDecimal]
        +totalConsultations: Int
    }
    
    class SpecialistConnection {
        +connectionTypeId: ConnectionTypeId
        +value: String
        +label: String
    }
    
    class Consultation {
        +ConsultationId: UUID
        +userId: UserId
        +specialistId: SpecialistId
        +categoryId: CategoryId
        +description: String
        +status: ConsultationStatus
        +scheduledAt: Instant
        +duration: Option[Int]
        +price: BigDecimal
        +rating: Option[Int]
        +review: Option[String]
        +createdAt: Instant
        +updatedAt: Instant
    }
    
    class Category {
        +CategoryId: UUID
        +name: String
        +description: String
        +parentId: Option[CategoryId]
    }
    
    class ConsultationStatus {
        <<enumeration>>
        Requested
        Scheduled
        InProgress
        Completed
        Missed
        Cancelled
    }
    
    class UserRole {
        <<enumeration>>
        Client
        Specialist
        Admin
    }
    
    User --> UserRole
    Specialist --> SpecialistCategoryRate
    Specialist --> SpecialistConnection
    Consultation --> ConsultationStatus
    Consultation --> User
    Consultation --> Specialist
    Consultation --> Category
    Specialist --> Category
    
    style User fill:#e1f5fe
    style Specialist fill:#fff3e0
    style Consultation fill:#e8f5e9
    style Category fill:#f3e5f5
```

---

## 7. Class Diagram - Service Layer

```mermaid
classDiagram
    class UserService {
        +create(request): Either[DomainError, User]
        +findById(id): Either[DomainError, User]
        +update(id, updates): Either[DomainError, User]
        +list(offset, limit): Either[DomainError, List[User]]
    }
    
    class SpecialistService {
        +create(request): Either[DomainError, Specialist]
        +findById(id): Either[DomainError, Specialist]
        +search(criteria): Either[DomainError, List[Specialist]]
        +updateAvailability(id, available): Either[DomainError, Specialist]
    }
    
    class ConsultationService {
        +create(request): Either[DomainError, Consultation]
        +findById(id): Either[DomainError, Consultation]
        +findByUser(userId): Either[DomainError, List[Consultation]]
        +findBySpecialist(specId): Either[DomainError, List[Consultation]]
        +updateStatus(id, status): Either[DomainError, Consultation]
        +addReview(id, rating, review): Either[DomainError, Consultation]
    }
    
    class CategoryService {
        +create(request): Either[DomainError, Category]
        +findById(id): Either[DomainError, Category]
        +listAll(): Either[DomainError, List[Category]]
        +listHierarchy(): Either[DomainError, CategoryTree]
    }
    
    class UserRepository {
        <<interface>>
        +save(user): F[Unit]
        +findById(id): F[Option[User]]
        +findByLogin(login): F[Option[User]]
        +findAll(offset, limit): F[List[User]]
    }
    
    class SpecialistRepository {
        <<interface>>
        +save(specialist): F[Unit]
        +findById(id): F[Option[Specialist]]
        +searchByCriteria(criteria): F[List[Specialist]]
    }
    
    class ConsultationRepository {
        <<interface>>
        +save(consultation): F[Unit]
        +findById(id): F[Option[Consultation]]
        +findByUser(userId): F[List[Consultation]]
        +findBySpecialist(specId): F[List[Consultation]]
    }
    
    class CategoryRepository {
        <<interface>>
        +save(category): F[Unit]
        +findById(id): F[Option[Category]]
        +findAll(): F[List[Category]]
    }
    
    UserService ..> UserRepository
    SpecialistService ..> SpecialistRepository
    ConsultationService ..> ConsultationRepository
    CategoryService ..> CategoryRepository
    
    style UserService fill:#e8f5e9
    style SpecialistService fill:#fff3e0
    style ConsultationService fill:#e1f5fe
    style CategoryService fill:#f3e5f5
```

---

## 8. Class Diagram - Ports & Adapters

```mermaid
classDiagram
    class RepositoryPorts {
        <<interface>>
        +userRepository: UserRepository
        +specialistRepository: SpecialistRepository
        +consultationRepository: ConsultationRepository
        +categoryRepository: CategoryRepository
        +credentialsRepository: CredentialsRepository
        +sessionRepository: SessionRepository
    }
    
    class InfrastructurePorts {
        <<interface>>
        +notificationService: NotificationService
        +storageService: StorageService
        +queueService: QueueService
    }
    
    class SecurityPorts {
        <<interface>>
        +authenticationService: AuthenticationService
        +tokenService: TokenService
        +passwordHashingService: PasswordHashingService
    }
    
    class PostgresUserRepository {
        -db: Transactor[F]
        +save(user): F[Unit]
        +findById(id): F[Option[User]]
        +findByLogin(login): F[Option[User]]
    }
    
    class PostgresSpecialistRepository {
        -db: Transactor[F]
        +save(specialist): F[Unit]
        +findById(id): F[Option[Specialist]]
        +searchByCriteria(criteria): F[List[Specialist]]
    }
    
    class AwsNotificationService {
        -snsClient: SnsClient
        -sesClient: SesClient
        +sendNotification(): F[Unit]
        +sendEmail(): F[Unit]
    }
    
    class MockNotificationService {
        +sendNotification(): F[Unit]
        +sendEmail(): F[Unit]
    }
    
    class JwtTokenService {
        -secret: String
        +generateToken(user): F[TokenPair]
        +verifyToken(token): F[Boolean]
    }
    
    class CacheService {
        <<interface>>
        +get(key): IO[Option[String]]
        +set(key, value, ttl): IO[Unit]
        +delete(key): IO[Unit]
    }
    
    class RedisCacheService {
        -db: Transactor[F]
        +get(key): IO[Option[String]]
        +set(key, value, ttl): IO[Unit]
        +delete(key): IO[Unit]
    }
    
    note for RedisCacheService "TODO: Not yet implemented\nCurrently returns noop/empty"
    
    RepositoryPorts <|-- PostgresUserRepository
    RepositoryPorts <|-- PostgresSpecialistRepository
    InfrastructurePorts <|-- AwsNotificationService
    InfrastructurePorts <|-- MockNotificationService
    SecurityPorts <|-- JwtTokenService
    InfrastructurePorts <|-- CacheService
    CacheService <|.. RedisCacheService
    
    style RepositoryPorts fill:#e1f5fe,stroke:#0288d1
    style InfrastructurePorts fill:#fff3e0,stroke:#f57c00
    style SecurityPorts fill:#f3e5f5,stroke:#7b1fa2
    style RedisCacheService fill:#ffebee,stroke:#c62828,stroke-dasharray: 5 5
```

---

## 9. Entity Relationship Diagram (Database Schema)

```mermaid
erDiagram
    USERS ||--o{ CONSULTATIONS : creates
    USERS ||--|| CREDENTIALS : has
    USERS ||--o{ SESSIONS : has
    USERS ||--o{ SECURITY_AUDIT : generates
    
    SPECIALISTS ||--o{ CONSULTATIONS : provides
    SPECIALISTS ||--o{ SPECIALIST_CATEGORIES : categorized_in
    SPECIALISTS ||--o{ SPECIALIST_CATEGORY_RATES : has_rates
    SPECIALISTS ||--o{ SPECIALIST_CONNECTIONS : has
    SPECIALISTS ||--o{ AVAILABILITY_SLOTS : defines
    
    CATEGORIES ||--o{ SPECIALIST_CATEGORIES : contains
    CATEGORIES ||--o{ SPECIALIST_CATEGORY_RATES : priced_in
    CATEGORIES ||--o{ CONSULTATIONS : consulted_in
    CATEGORIES ||--o{ CATEGORIES : parent_child
    
    CONSULTATIONS ||--|| CONSULTATION_REVIEWS : reviewed_as
    
    CONNECTION_TYPES ||--o{ SPECIALIST_CONNECTIONS : used_by
    
    NOTIFICATION_PREFERENCES ||--|| USERS : belongs_to
    
    REFRESH_TOKENS ||--|| CREDENTIALS : associated_with
    
    USERS {
        UUID id PK
        String login UK
        String email UK
        String name
        String phone
        UserRole role
        UUID country_id FK
        Timestamp created_at
        Timestamp updated_at
    }
    
    CREDENTIALS {
        UUID id PK
        UUID user_id UK FK
        String password_hash
        Timestamp created_at
        Timestamp updated_at
    }
    
    SPECIALISTS {
        UUID id PK
        String email UK
        String name
        String phone
        String bio
        Boolean is_available
        UUID country_id FK
        Timestamp created_at
        Timestamp updated_at
    }
    
    SPECIALIST_CATEGORY_RATES {
        UUID specialist_id PK FK
        UUID category_id PK FK
        Decimal hourly_rate
        Int experience_years
        Decimal rating
        Int total_consultations
    }
    
    CATEGORIES {
        UUID id PK
        String name UK
        String description
        UUID parent_id FK
    }
    
    CONSULTATIONS {
        UUID id PK
        UUID user_id FK
        UUID specialist_id FK
        UUID category_id FK
        String description
        ConsultationStatus status
        Timestamp scheduled_at
        Int duration
        Decimal price
        Timestamp created_at
        Timestamp updated_at
    }
    
    SPECIALIST_CONNECTIONS {
        UUID specialist_id PK FK
        UUID connection_type_id PK FK
        String value
        String label
    }
    
    CONNECTION_TYPES {
        UUID id PK
        String name UK
        String icon
    }
    
    AVAILABILITY_SLOTS {
        UUID id PK
        UUID specialist_id FK
        Timestamp start_time
        Timestamp end_time
        Boolean is_booked
    }
    
    NOTIFICATION_PREFERENCES {
        UUID user_id PK FK
        Boolean email_enabled
        Boolean sms_enabled
        Boolean push_enabled
    }
```

---

## 10. State Diagram - Consultation Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Requested: Client creates
    
    Requested --> Scheduled: Specialist approves<br/>sets duration & time
    Requested --> Cancelled: Client cancels<br/>or Specialist rejects
    
    Scheduled --> InProgress: Consultation starts
    Scheduled --> Cancelled: Either party cancels
    Scheduled --> Missed: No-show
    
    InProgress --> Completed: Session ends
    InProgress --> Cancelled: Emergency cancellation
    
    Completed --> [*]: Review added
    Missed --> [*]: Resolved
    Cancelled --> [*]: Closed
    
    note right of Requested
        Initial state
        Waiting for specialist
        to respond
    end note
    
    note right of Scheduled
        Time & duration set
        Both parties notified
        Ready to begin
    end note
    
    note right of InProgress
        Consultation active
        Timer running
    end note
    
    note right of Completed
        Final state
        Review & rating
        can be added
    end note
    
    state Requested {
        [*] --> PendingReview
        PendingReview --> PendingApproval
    }
    
    state Completed {
        [*] --> AwaitingReview
        AwaitingReview --> Reviewed
        Reviewed --> [*]
    }
```

---

## 11. Flowchart - Specialist Registration Process

```mermaid
flowchart TD
    Start([Start Registration]) --> Validate[Validate Input Data]
    
    Validate --> CheckEmail{Email Already<br/>Exists?}
    CheckEmail -->|Yes| EmailError[Return "Email Already Exists" Error]
    CheckEmail -->|No| CheckLogin{Login Already<br/>Exists?}
    
    CheckLogin -->|Yes| LoginError[Return "Login Already Exists" Error]
    CheckLogin -->|No| ValidatePassword[Validate Password<br/>Strength]
    
    ValidatePassword --> PassValid{Password<br/>Valid?}
    PassValid -->|No| PassError[Return "Weak Password" Error]
    PassValid -->|Yes| CreateCreds[Create Credentials<br/>Hash Password]
    
    CreateCreds --> CreateUser[Create User Record<br/>Role = Specialist]
    CreateUser --> CreateSpec[Create Specialist Record<br/>Basic Info]
    
    CreateSpec --> CheckCategories{Has Categories?}
    CheckCategories -->|Yes| ValidateCats[Validate Category IDs<br/>Exist in DB]
    ValidateCats --> SaveRates[Save SpecialistCategoryRates<br/>Hourly Rate & Experience]
    CheckCategories -->|No| SkipRates[Skip Category Rates]
    
    SaveRates --> CheckConnections{Has Connections?}
    SkipRates --> CheckConnections
    
    CheckConnections -->|Yes| ValidateConn[Validate Connection Types<br/>WhatsApp, Viber, etc.]
    ValidateConn --> SaveConn[Save SpecialistConnections<br/>Contact Details]
    CheckConnections -->|No| SkipConn[Skip Connections]
    
    SaveConn --> SetAvailable[Set isAvailable = true<br/>Default Status]
    SkipConn --> SetAvailable
    
    SetAvailable --> SendWelcome[Send Welcome Email<br/>via NotificationService]
    SendWelcome --> ReturnSpec[Return Specialist DTO<br/>with All Relations]
    ReturnSpec --> End([End])
    
    EmailError --> End
    LoginError --> End
    PassError --> End
    
    style Start fill:#4caf50,color:#fff
    style End fill:#f44336,color:#fff
    style CheckEmail fill:#ff9800
    style CheckLogin fill:#ff9800
    style ValidatePassword fill:#ff9800
    style PassValid fill:#ff9800
    style CheckCategories fill:#ff9800
    style CheckConnections fill:#ff9800
    style ReturnSpec fill:#2196f3,color:#fff
```

---

## 12. Flowchart - Authentication & Authorization Flow

```mermaid
flowchart TD
    Request[Incoming HTTP Request] --> IsPublic{Public Endpoint?<br/>/health, /docs, /register}
    
    IsPublic -->|Yes| Allow[Allow Request<br/>No Auth Required]
    Allow --> Handler[Route Handler]
    
    IsPublic -->|No| HasToken{Has Authorization<br/>Header?}
    
    HasToken -->|No| AuthError[Return 401<br/>Unauthorized]
    
    HasToken -->|Yes| ValidateJWT[Validate JWT Token<br/>Signature & Expiry]
    
    ValidateJWT --> TokenValid{Token Valid?}
    TokenValid -->|No| AuthError
    
    TokenValid -->|Yes| ExtractClaims[Extract Claims<br/>userId, role, exp]
    ExtractClaims --> CheckRole{Required Role<br/>Matches?}
    
    CheckRole -->|No| Forbidden[Return 403<br/>Forbidden]
    
    CheckRole -->|Yes| LoadUser[Load User from DB<br/>Verify Active Session]
    
    LoadUser --> UserExists{User Exists<br/>& Active?}
    UserExists -->|No| SessionError[Return 401<br/>Invalid Session]
    
    UserExists -->|Yes| InjectUser[Inject User into<br/>Request Context]
    InjectUser --> Handler
    
    Handler --> Response[Generate Response]
    Response --> LogAudit[Log Security Audit<br/>Access Record]
    LogAudit --> ReturnResp[Return to Client]
    
    style Request fill:#2196f3,color:#fff
    style IsPublic fill:#ff9800
    style HasToken fill:#ff9800
    style TokenValid fill:#ff9800
    style CheckRole fill:#ff9800
    style UserExists fill:#ff9800
    style Allow fill:#4caf50,color:#fff
    style AuthError fill:#f44336,color:#fff
    style Forbidden fill:#f44336,color:#fff
    style SessionError fill:#f44336,color:#fff
    style Handler fill:#9c27b0,color:#fff
```

---

## 13. Deployment Diagram - Docker Compose Setup

```mermaid
flowchart TB
    subgraph External["External Clients"]
        ClientApp[Client App<br/>localhost:3000]
        AdminApp[Admin App<br/>localhost:3001]
        SpecApp[Specialist App<br/>localhost:3002]
        Swagger[Swagger UI<br/>Browser]
    end
    
    subgraph Docker["Docker Network"]
        direction TB
        
        subgraph Nginx["Nginx Reverse Proxy"]
            NGINX[Nginx Container<br/>Port 9443 HTTPS<br/>Port 9080 HTTP]
        end
        
        subgraph AppLayer["Application Layer"]
            API1[API Instance 1<br/>Port 8081]
            API2[API Instance 2<br/>Port 8082]
            API3[API Instance 3<br/>Port 8083]
        end
        
        subgraph DataLayer["Data Layer"]
            PG[(PostgreSQL<br/>Port 5432)]
        end
        
        subgraph LocalStack["LocalStack (AWS Mock)"]
            S3[S3 Bucket]
            SQS[SQS Queue]
            SNS[SNS Topic]
            SES[SES Email]
        end
        
        subgraph Future["Future/Planned"]
            REDIS[(Redis Cache<br/>Not Yet Implemented)]
        end
    end
    
    ClientApp -->|HTTPS| NGINX
    AdminApp -->|HTTPS| NGINX
    SpecApp -->|HTTPS| NGINX
    Swagger -->|HTTPS| NGINX
    
    NGINX -->|Load Balance| API1
    NGINX -->|Load Balance| API2
    NGINX -->|Load Balance| API3
    
    API1 --> PG
    API2 --> PG
    API3 --> PG
    
    API1 --> LocalStack
    API2 --> LocalStack
    API3 --> LocalStack
    
    API1 -.->|Future| REDIS
    API2 -.->|Future| REDIS
    API3 -.->|Future| REDIS
    
    style External fill:#f5f5f5,stroke:#666
    style Docker fill:#e1f5fe,stroke:#0288d1
    style Nginx fill:#4caf50,color:#fff
    style AppLayer fill:#fff3e0,stroke:#f57c00
    style DataLayer fill:#e8f5e9,stroke:#388e3c
    style LocalStack fill:#f3e5f5,stroke:#7b1fa2
    style Future fill:#ffebee,stroke:#c62828,stroke-dasharray: 5 5
```

---

## 14. Component Diagram - Full System Architecture

```mermaid
flowchart TB
    subgraph Frontend["Frontend Applications"]
        CA[Client App<br/>Nuxt.js]
        AA[Admin App<br/>Nuxt.js]
        SA[Specialist App<br/>Nuxt.js]
    end
    
    subgraph APIGateway["API Gateway Layer"]
        NGINX[Nginx<br/>HTTPS/TLS<br/>Load Balancer]
    end
    
    subgraph Backend["Backend Services"]
        subgraph API["API Module (api)"]
            ROUTES[Tapir Routes<br/>Authentication<br/>Specialist<br/>Consultation<br/>User<br/>Category<br/>Connection]
            DTO[DTOs & Mappers]
            MW[Middleware<br/>Auth<br/>Logging<br/>Error Handling]
        end
        
        subgraph Core["Core Module (core)"]
            DOMAIN[Domain Models<br/>User, Specialist<br/>Consultation, Category]
            SVC[Business Services<br/>Validation Logic<br/>Business Rules]
            PORTS[Port Interfaces<br/>Repository Ports<br/>Infrastructure Ports]
        end
        
        subgraph Data["Data Module (data)"]
            REPO[Doobie Repositories<br/>SQL Queries<br/>Transaction Mgmt]
            MIGRATION[Flyway Migrations<br/>Schema Evolution<br/>Seed Data]
            DB[(PostgreSQL<br/>Primary Database)]
        end
        
        subgraph Infra["Infrastructure Module (infrastructure)"]
            AWS[AWS Adapters<br/>S3, SNS, SQS, SES<br/>via LocalStack]
            LOCAL[Local Adapters<br/>Mock Services]
            SEC[Security Services<br/>JWT, OIDC, BCrypt]
            METRICS[Metrics & Monitoring<br/>Health Checks]
        end
    end
    
    subgraph External["External Services"]
        LOCALSTACK[LocalStack<br/>AWS Mock]
    end
    
    CA --> NGINX
    AA --> NGINX
    SA --> NGINX
    
    NGINX --> ROUTES
    ROUTES --> MW
    MW --> DTO
    DTO --> SVC
    SVC --> DOMAIN
    DOMAIN --> PORTS
    
    PORTS --> REPO
    PORTS --> AWS
    PORTS --> SEC
    PORTS --> LOCAL
    
    REPO --> MIGRATION
    MIGRATION --> DB
    
    AWS --> LOCALSTACK
    
    LOCAL -.-> AWS
    SEC -.-> LOCAL
    
    style Frontend fill:#e3f2fd,stroke:#1976d2
    style APIGateway fill:#4caf50,color:#fff
    style Backend fill:#fff3e0,stroke:#f57c00
    style API fill:#e1f5fe,stroke:#0288d1
    style Core fill:#f3e5f5,stroke:#7b1fa2
    style Data fill:#e8f5e9,stroke:#388e3c
    style Infra fill:#fff9c4,stroke:#fbc02d
    style External fill:#f5f5f5,stroke:#666
```

---

## 15. Package Dependency Diagram

```mermaid
flowchart LR
    subgraph Projects["SBT Subprojects"]
        API[api<br/>• HTTP Routes<br/>• DTOs<br/>• Server]
        CORE[core<br/>• Domain Models<br/>• Services<br/>• Ports]
        DATA[data<br/>• Repositories<br/>• Migrations<br/>• Doobie]
        INFRA[infrastructure<br/>• AWS Adapters<br/>• Security<br/>• Cache (stub)]
    end
    
    subgraph Libraries["Key Libraries"]
        HTTP4S[http4s<br/>Ember Server]
        TAPIR[tapir<br/>API Definitions]
        CATS[cats-effect<br/>Functional Programming]
        DOOBIE[doobie<br/>Database Access]
        CIRCE[circe<br/>JSON Handling]
        CIRIS[ciris<br/>Configuration]
        JWT[jwt-scala<br/>Token Management]
        AWS[aws-sdk<br/>AWS Services]
    end
    
    API --> CORE
    API --> DATA
    API --> INFRA
    
    DATA --> CORE
    INFRA --> CORE
    
    API --> HTTP4S & TAPIR & CIRCE
    CORE --> CATS
    DATA --> DOOBIE & CIRCE
    INFRA --> AWS & JWT & CIRIS
    
    style API fill:#e1f5fe,stroke:#0288d1
    style CORE fill:#f3e5f5,stroke:#7b1fa2
    style DATA fill:#e8f5e9,stroke:#388e3c
    style INFRA fill:#fff3e0,stroke:#f57c00
```

---

## Summary

These diagrams illustrate the complete architecture of the Consultant Backend system:

1. **Project Structure** - Shows the 4 subprojects and their dependencies
2. **Hexagonal Architecture** - Demonstrates the ports & adapters pattern
3. **Authentication Flow** - Sequence of login/token generation
4. **Search Specialists** - Query flow (no caching currently)
5. **Create Consultation** - Booking workflow with notifications
6. **Domain Models** - Core business entities and relationships
7. **Service Layer** - Business services and repository interfaces
8. **Ports & Adapters** - Implementation of hexagonal architecture
9. **ER Diagram** - Complete database schema
10. **Consultation States** - Lifecycle from request to completion
11. **Specialist Registration** - Input validation (email, login, password) and data flow
12. **Authentication Flow** - Security middleware processing
13. **Docker Deployment** - Container orchestration
14. **Component Architecture** - Full system overview
15. **Package Dependencies** - SBT project and library dependencies

**Notes:**
- **Redis**: Currently NOT implemented. `RedisCacheService` exists as a stub with noop implementations. Planned for future.
- **Notifications**: Uses `MockNotificationService` locally; AWS SES/SNS available for production.
- **Storage**: Uses `LocalStorageService` locally; AWS S3 available for production.

All diagrams follow Mermaid syntax and can be rendered in GitHub, GitLab, or any Mermaid-compatible viewer.
