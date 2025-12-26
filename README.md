# Selah API Server

> "멈추고, 묵상하고, 기록하다"

기도제목과 기도문을 기록하고, 응답받은 기도를 확인하며 믿음을 성장시키는 **개인용 기도노트 서비스** Selah의 백엔드 API 서버입니다.

---

## 프로젝트 개요

### 핵심 가치

| Value        | Description                 |
|--------------|-----------------------------|
| **간편한 기록**   | 기도제목을 제목만으로 빠르게 기록          |
| **기도 습관 형성** | 매일 기도문을 작성하며 기도 생활 유지       |
| **믿음 성장**    | 응답받은 기도를 확인하며 하나님에 대한 신뢰 강화 |

### 주요 기능

- **기도제목 관리**: 기도제목 CRUD, 응답 체크, 소감 작성
- **기도문 관리**: 기도문 작성 (하루 여러 개 가능), 히스토리 조회
- **돌아보기**: 과거 기도제목 리마인드, 랜덤/주기적 선정
- **통계**: 응답된 기도 통계, 타임라인 뷰
- **인증**: 소셜 로그인 (Google, Apple, 카카오) + 이메일/비밀번호

---

## Tech Stack

| Category     | Technology        | Version      |
|--------------|-------------------|--------------|
| Language     | Kotlin            | 2.x          |
| JDK          | OpenJDK           | 21           |
| Framework    | Spring Boot       | 4.x          |
| Build        | Gradle            | Kotlin DSL   |
| Architecture | Hexagonal + DDD   | -            |
| Security     | Spring Security   | OAuth2 + JWT |
| Persistence  | Spring Data JPA   | -            |
| Database     | PostgreSQL        | (TBD)        |
| Async        | Kotlin Coroutines | -            |
| Testing      | Kotest, MockK     | -            |

---

## Architecture

### Hexagonal Architecture (Ports and Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                      Adapter Layer                          │
│  ┌─────────────────┐              ┌─────────────────────┐  │
│  │    Inbound      │              │      Outbound       │  │
│  │  (Controller)   │              │   (Repository)      │  │
│  └────────┬────────┘              └──────────┬──────────┘  │
└───────────┼──────────────────────────────────┼─────────────┘
            │                                  │
            ▼                                  ▼
┌───────────────────────────────────────────────────────────┐
│                   Application Layer                        │
│  ┌─────────────────┐              ┌─────────────────────┐ │
│  │  Inbound Port   │              │   Outbound Port     │ │
│  │   (UseCase)     │              │   (Repository IF)   │ │
│  └────────┬────────┘              └──────────┬──────────┘ │
│           │         Service                  │            │
│           └──────────────────────────────────┘            │
└───────────────────────────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────┐
│                     Domain Layer                           │
│         (Entities, Value Objects, Domain Events)          │
│                   순수 Kotlin (POKO)                       │
└───────────────────────────────────────────────────────────┘
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

| 책임     | 설명                         |
|--------|----------------------------|
| 인증/인가  | OAuth 2.0 소셜 로그인 + 이메일 로그인 |
| JWT 관리 | 토큰 발급, 검증, 갱신              |
| 프로필 관리 | 닉네임, 프로필 이미지 등             |

**Aggregates**: `Member`

### 2. Prayer Context (기도 도메인)

| 책임      | 설명               |
|---------|------------------|
| 기도제목 관리 | 기도제목 CRUD, 응답 체크 |
| 기도문 관리  | 기도문 작성, 조회       |
| 돌아보기    | 과거 기도제목 선정 및 알림  |
| 통계      | 응답된 기도 통계 제공     |

**Aggregates**: `PrayerTopic`, `Prayer`

---

## Package Structure

```
io.clroot.selah
├── common/                     # 공통 유틸리티, 전역 예외 처리
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
    ├── member/                 # 회원 컨텍스트
    │   ├── adapter/
    │   │   ├── inbound/       # REST Controller
    │   │   │   └── web/
    │   │   └── outbound/      # JPA Repository
    │   │       └── persistence/
    │   ├── application/
    │   │   ├── port/
    │   │   │   ├── inbound/   # UseCase Interfaces
    │   │   │   └── outbound/  # Repository Port Interfaces
    │   │   └── service/       # UseCase Implementations
    │   └── domain/
    │       ├── model/         # Entities, Value Objects
    │       └── event/         # Domain Events
    │
    └── prayer/                 # 기도 컨텍스트
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

## API Endpoints (예정)

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

# 2. Start database (Docker)
docker-compose up -d

# 3. Run application
./gradlew bootRun

# 4. Access API
curl http://localhost:8080/api/v1/health
```

### Build

```bash
# Build
./gradlew build

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test
```

---

## Development Guidelines

자세한 개발 가이드라인은 [CLAUDE.md](./CLAUDE.md) 참조

### 핵심 규칙

- Domain 클래스는 외부 라이브러리에 의존하지 않음
- Aggregate Root는 `AggregateRoot<ID>` 상속
- 상태 변경은 비즈니스 메서드를 통해서만 (캡슐화)
- JPA Entity ↔ Domain Model 매핑은 Mapper로 분리
- JPA 호출은 `withContext(Dispatchers.IO)` 내부에서
- 테스트는 Kotest + MockK 사용

---

## Related Projects

| Project                         | Description             |
|---------------------------------|-------------------------|
| [selah-web](../selah-web)       | Next.js 웹 애플리케이션        |
| [selah-mobile](../selah-mobile) | React Native 모바일 앱 (예정) |

---

## License

MIT License
