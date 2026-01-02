# Selah API Server

> "멈추고, 묵상하고, 기록하다"

기도제목과 기도문을 기록하고, 응답의 과정을 확인하며 나만의 기도의 여정을 기록하는 **개인용 기도노트 서비스** Selah의 백엔드 API 서버입니다.

---

## 프로젝트 개요

### 주요 기능

- **기도제목 관리**: 기도제목 CRUD, 응답 체크, 소감 작성
- **기도문 관리**: 기도문 작성 (하루 여러 개 가능), 히스토리 조회
- **돌아보기**: 과거 기도제목 리마인드, 랜덤/주기적 선정
- **통계**: 응답된 기도 통계, 타임라인 뷰
- **인증**: 소셜 로그인 (Google, Apple, 카카오) + 이메일/비밀번호
- **E2E 암호화**: 기도 데이터 클라이언트 측 암호화 (서버는 평문 접근 불가)

---

## Tech Stack

| Category     | Technology        | Version    |
|--------------|-------------------|------------|
| Language     | Kotlin            | 2.2.20     |
| JDK          | OpenJDK           | 21         |
| Framework    | Spring Boot       | 4.0.0      |
| Build        | Gradle            | Kotlin DSL |
| Architecture | Hexagonal + DDD   | -          |
| Security     | Spring Security   | OAuth2     |
| Persistence  | Spring Data JPA   | -          |
| Database     | PostgreSQL        | -          |
| Async        | Kotlin Coroutines | 1.10.2     |
| Testing      | Kotest, MockK     | -          |

---

## Architecture

### Hexagonal Architecture (Ports and Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                        Adapter Layer                        │
│  ┌─────────────────┐              ┌─────────────────────┐   │
│  │    Inbound      │              │      Outbound       │   │
│  │  (Controller)   │              │   (Repository)      │   │
│  └────────┬────────┘              └──────────┬──────────┘   │
└───────────┼──────────────────────────────────┼──────────────┘
            │                                  │
            ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                      │
│  ┌─────────────────┐              ┌─────────────────────┐   │
│  │  Inbound Port   │              │    Outbound Port    │   │
│  │    (UseCase)    │              │   (Repository IF)   │   │
│  └────────┬────────┘              └──────────┬──────────┘   │
│           │            Service               │              │
│           └──────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                         │
│          (Entities, Value Objects, Domain Events)           │
│                     Pure Kotlin (POKO)                      │
└─────────────────────────────────────────────────────────────┘
```

### 의존성 방향

```
Adapter → Application → Domain
```

- **Domain Layer**: 외부 라이브러리 의존 금지 (순수 Kotlin)
- **Application Layer**: Domain과 Port에만 의존
- **Adapter Layer**: 외부 세계와의 통신 담당

---

## Bounded Contexts

### 1. Member Context (회원 도메인)

| 책임          | 설명                                |
|-------------|-----------------------------------|
| 인증/인가       | OAuth 2.0 소셜 로그인 + 이메일 로그인        |
| 프로필 관리      | 닉네임, 프로필 이미지 등                    |
| OAuth 연동 관리 | 소셜 계정 연결/해제                       |
| E2E 암호화 설정  | Salt, encryptedDEK, Server Key 관리 |

**Aggregates**: `Member`, `OAuthConnection`, `EncryptionSettings`, `ServerKey`

### 2. Prayer Context (기도 도메인)

| 책임      | 설명               |
|---------|------------------|
| 기도제목 관리 | 기도제목 CRUD, 응답 체크 |
| 기도문 관리  | 기도문 작성, 조회       |
| 돌아보기    | 과거 기도제목 선정 및 알림  |
| 통계      | 응답된 기도 통계 제공     |
| E2E 암호화 | 클라이언트 암호화 데이터 저장 |

**Aggregates**: `PrayerTopic`, `Prayer`

> **🔒 E2E 암호화**: 기도제목(title, reflection)과 기도문(content)은 클라이언트에서 암호화되어 전송됩니다. 서버는 암호문(Base64)만 저장하며, 평문에 접근할 수 없습니다. 상세 설계는 [Selah Web 레포지토리의 E2E 명세](https://github.com/clroot/selah-web-application/blob/main/docs/E2E_ENCRYPTION.md)를 참조하세요.

---

## Package Structure

```
io.clroot.selah
├── common/                     # Common utilities, global error handling
│   ├── domain/
│   │   ├── AggregateRoot.kt
│   │   └── AggregateId.kt
│   ├── event/
│   │   ├── DomainEvent.kt
│   │   └── IntegrationEvent.kt
│   └── exception/
│       └── DomainException.kt
│
└── domains/
    ├── member/                 # Member Context
    │   ├── adapter/
    │   │   ├── inbound/        # REST Controllers
    │   │   │   └── web/
    │   │   └── outbound/       # JPA Repositories
    │   │       └── persistence/
    │   ├── application/
    │   │   ├── port/
    │   │   │   ├── inbound/    # UseCase Interfaces
    │   │   │   └── outbound/   # Repository Port Interfaces
    │   │   └── service/        # UseCase Implementations
    │   └── domain/
    │       ├── model/          # Entities, Value Objects
    │       └── event/          # Domain Events
    │
    └── prayer/                 # Prayer Context
        ├── adapter/
        │   ├── inbound/
        │   └── outbound/
        ├── application/
        │   ├── port/
        │   └── service/
        └── domain/
            ├── model/
            └── event/
```

---

## API Endpoints

### Auth

| Method | Endpoint                        | Description |
|--------|---------------------------------|-------------|
| POST   | `/api/v1/auth/signup`           | 이메일 회원가입    |
| POST   | `/api/v1/auth/login`            | 이메일 로그인     |
| POST   | `/api/v1/auth/oauth/{provider}` | 소셜 로그인      |
| POST   | `/api/v1/auth/refresh`          | 토큰 갱신       |

### Prayer Topics

| Method | Endpoint                            | Description |
|--------|-------------------------------------|-------------|
| GET    | `/api/v1/prayer-topics`             | 기도제목 목록 조회  |
| POST   | `/api/v1/prayer-topics`             | 기도제목 생성     |
| GET    | `/api/v1/prayer-topics/{id}`        | 기도제목 상세 조회  |
| PATCH  | `/api/v1/prayer-topics/{id}`        | 기도제목 수정     |
| DELETE | `/api/v1/prayer-topics/{id}`        | 기도제목 삭제     |
| PATCH  | `/api/v1/prayer-topics/{id}/answer` | 응답 체크       |

### Prayers

| Method | Endpoint               | Description |
|--------|------------------------|-------------|
| GET    | `/api/v1/prayers`      | 기도문 목록 조회   |
| POST   | `/api/v1/prayers`      | 기도문 작성      |
| GET    | `/api/v1/prayers/{id}` | 기도문 상세 조회   |
| DELETE | `/api/v1/prayers/{id}` | 기도문 삭제      |

### Reflection

| Method | Endpoint                      | Description |
|--------|-------------------------------|-------------|
| GET    | `/api/v1/reflection/today`    | 오늘의 돌아볼 기도  |
| GET    | `/api/v1/reflection/answered` | 응답된 기도 목록   |
| GET    | `/api/v1/reflection/stats`    | 통계 조회       |
| GET    | `/api/v1/reflection/timeline` | 타임라인 조회     |

---

## Getting Started

### Prerequisites

- JDK 21
- Docker & Docker Compose (for local DB)

### Run Locally

```bash
# 1. Clone repository
git clone https://github.com/clroot/selah-api-server.git
cd selah-api-server

# 2. Start services (PostgreSQL, Mailpit)
docker-compose up -d

# 3. Run application
./gradlew bootRun
```

### Infrastructure Services

| Service              | Port | Description                                                   |
|----------------------|------|---------------------------------------------------------------|
| **PostgreSQL**       | 5432 | Database (selah/selah/selah)                                  |
| **Mailpit (SMTP)**   | 1025 | Local mail testing server                                     |
| **Mailpit (Web UI)** | 8025 | Mail dashboard [http://localhost:8025](http://localhost:8025) |

### Build

```bash
# Build
./gradlew build

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Lint Check & Format
./gradlew ktlintCheck
./gradlew ktlintFormat
```

---

## Development Guidelines

자세한 개발 가이드라인은 [CLAUDE.md](./CLAUDE.md) 참조

### 아키텍처 가이드 (Architectural Design Notes)
프로젝트 개발 과정에서의 주요 기술적 도전과 이를 해결하기 위한 아키텍처 설계 결정을 기록합니다.
- [Kotlin Coroutines 통합 아키텍처 가이드](./docs/architecture/kotlin-coroutines-integration.md)

### 핵심 규칙
- Domain 클래스는 외부 라이브러리에 의존하지 않음
- Aggregate Root는 `AggregateRoot<ID>` 상속
- 상태 변경은 비즈니스 메서드를 통해서만 (캡슐화)
- JPA Entity ↔ Domain Model 매핑은 Mapper로 분리
- JPA 호출은 `withContext(Dispatchers.IO)` 내부에서
- 테스트는 Kotest + MockK 사용

---

## Related Projects

| Project       | Description      | URL                                                                                                |
|---------------|------------------|----------------------------------------------------------------------------------------------------|
| **Selah Web** | Next.js 웹 애플리케이션 | [https://github.com/clroot/selah-web-application](https://github.com/clroot/selah-web-application) |

---

## License

MIT License
