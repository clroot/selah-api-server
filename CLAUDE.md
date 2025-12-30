# Selah API Server - Claude Code Guidelines

> "멈추고, 묵상하고, 기록하다"
>
> 기도제목과 기도문을 기록하고, 응답받은 기도를 확인하며 믿음을 성장시키는 개인용 기도노트 서비스

## 프로젝트 개요

### 핵심 가치

| Value | Description |
|-------|-------------|
| **간편한 기록** | 기도제목을 제목만으로 빠르게 기록 |
| **기도 습관 형성** | 매일 기도문을 작성하며 기도 생활 유지 |
| **믿음 성장** | 응답받은 기도를 확인하며 하나님에 대한 신뢰 강화 |

### Architecture & Design

- **Architecture**: Hexagonal Architecture (Ports and Adapters)
- **Design Pattern**: Domain-Driven Design (DDD)

## Tech Stack

| Category | Technology | Version/Note |
|----------|------------|--------------|
| Language | Kotlin | 2.x (JDK 21) |
| Framework | Spring Boot | 4.x (Spring 6.x) |
| Build | Gradle | Kotlin DSL |
| Persistence | Spring Data JPA | Hibernate |
| Query DSL | Kotlin JDSL | 타입 안전한 쿼리 |
| Security | Spring Security | OAuth2 + JWT |
| Async | Kotlin Coroutines, Virtual Threads | 비동기 처리 |
| Logging | kotlin-logging | SLF4J 래퍼 |
| Testing | Kotest, MockK | Spec 스타일 |

### 동시성 설정

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

## 🚨 Critical Architecture Rules

### 1. 의존성 방향 (절대 위반 금지)

```
Adapter → Application → Domain
```

- **Domain Layer**: 외부 라이브러리 의존 금지 (Spring, JPA, HTTP Client 등). 순수 POKO 유지
- **Application Layer**: Domain과 Port에만 의존
- **Adapter Layer**: 외부 세계와의 통신 담당

### 2. 매핑 전략 (철저히 준수)

```
Web Request DTO → Command (Application)
JPA Entity (Adapter) ↔ Domain Model (Domain)  # 반드시 Mapper로 분리
```

**이유**: JPA 어노테이션(`@Entity`)이 도메인 모델을 오염시키지 않도록 함

## Bounded Contexts

### 👤 1. Member Context (회원 도메인)

**역할**: "사용자 매니저"

| 책임 | 설명 |
|------|------|
| 인증/인가 | OAuth 2.0 소셜 로그인 + 이메일 로그인 |
| JWT 관리 | 토큰 발급, 검증, 갱신 |
| 프로필 관리 | 닉네임, 프로필 이미지 등 |
| OAuth 연동 관리 | 소셜 계정 연결/해제 |

**Aggregates**: `Member`(Root), `OAuthConnection`(Root)

> **📌 OAuthConnection 분리**: `OAuthConnection`은 `Member`와 별도의 Aggregate로 관리됩니다. `memberId`를 통해 소속을 식별하며, 로그인/연동 관리 시 독립적으로 조회됩니다. 불변식(동일 Member에 동일 Provider 하나만)은 DB Unique 제약조건으로 보장합니다.

### 🙏 2. Prayer Context (기도 도메인)

**역할**: "기도 관리자"

| 책임 | 설명 |
|------|------|
| 기도제목 관리 | 기도제목 CRUD, 응답 체크 |
| 기도문 관리 | 기도문 작성, 조회 |
| 돌아보기 | 과거 기도제목 선정 및 알림 |
| 통계 | 응답된 기도 통계 제공 |
| E2E 암호화 | 클라이언트 암호화 데이터 저장/조회 |

**Aggregates**: `PrayerTopic`(Root), `Prayer`(Root)

> **🔒 E2E 암호화 지원**: 기도제목(title, reflection)과 기도문(content)은 클라이언트에서 AES-256-GCM으로 암호화되어 전송됩니다. 서버는 암호문(Base64)만 저장하며, 평문에 접근할 수 없습니다. 자세한 내용은 [E2E 암호화 - Backend 역할](#e2e-암호화---backend-역할) 섹션 참조.

---

## Context 간 통신 규칙

### 이벤트 종류

| 종류 | 목적 | 위치 | 페이로드 | 핸들러 위치 |
|------|------|------|----------|------------|
| **Domain Event** | 같은 Context 내부 통신 | `domain/event/` | Domain 객체 포함 가능 | `application/listener/` |
| **Integration Event** | Context 간 통신 | `application/event/` | Snapshot DTO만 사용 | `adapter/inbound/event/` |

### 이벤트 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                     Member Context                           │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Domain Layer                                          │  │
│  │                                                       │  │
│  │   Member.createWithEmail() ──▶ MemberRegisteredEvent  │  │
│  │                                       │               │  │
│  └───────────────────────────────────────┼───────────────┘  │
│                                          ▼                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Application Layer                                     │  │
│  │                                                       │  │
│  │   listener/                                           │  │
│  │   └── MemberRegisteredEventListener ◀── 같은 Context  │  │
│  │           │                              내부 이벤트   │  │
│  │           └── 이메일 인증 메일 발송                     │  │
│  │                                                       │  │
│  │   service/                                            │  │
│  │   └── RegisterMemberService                           │  │
│  │           │                                           │  │
│  │           └── Integration Event 발행 (필요시)          │  │
│  │               ──▶ MemberCreatedIntegrationEvent       │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼  Context 경계
┌─────────────────────────────────────────────────────────────┐
│                     Prayer Context                           │
│                                                             │
│   adapter/inbound/event/  ◀── 다른 Context에서 온 이벤트     │
│   └── MemberCreatedEventHandler                              │
│               │                                              │
│               └──▶ UseCase 호출 (필요시)                      │
└─────────────────────────────────────────────────────────────┘
```

### 핸들러 위치 규칙

| 이벤트 발행 Context | 이벤트 처리 Context | 핸들러 위치 |
|-------------------|-------------------|------------|
| Member | Member (같은 Context) | `application/listener/` |
| Member | Prayer (다른 Context) | `adapter/inbound/event/` |
| Prayer | Prayer (같은 Context) | `application/listener/` |
| Prayer | Member (다른 Context) | `adapter/inbound/event/` |

### 패키지 구조 (이벤트 포함)

```
io.clroot.selah
├── common/
│   ├── domain/
│   │   └── AggregateRoot.kt
│   └── event/
│       ├── DomainEvent.kt            # Domain Event 마커 인터페이스
│       └── IntegrationEvent.kt       # Integration Event 마커 인터페이스
│
└── domains/
    ├── member/
    │   ├── domain/
    │   │   ├── Member.kt
    │   │   └── event/                # 🔵 Domain Event (내부용)
    │   │       ├── MemberRegisteredEvent.kt
    │   │       └── EmailVerifiedEvent.kt
    │   │
    │   ├── application/
    │   │   ├── event/                # 🟢 Integration Event (외부용, 필요시)
    │   │   │   └── MemberCreatedIntegrationEvent.kt
    │   │   ├── listener/             # 🟣 같은 Context 이벤트 리스너
    │   │   │   └── MemberRegisteredEventListener.kt
    │   │   ├── port/
    │   │   └── service/
    │   │
    │   └── adapter/
    │       └── inbound/
    │           └── event/            # 🟣 다른 Context 이벤트 핸들러 (필요시)
    │
    └── prayer/
        ├── domain/
        │   └── event/
        ├── application/
        │   ├── listener/             # 🟣 같은 Context 이벤트 리스너
        │   └── service/
        └── adapter/
            └── inbound/
                └── event/            # 🟣 다른 Context 이벤트 핸들러
                    └── MemberCreatedEventHandler.kt  # Member → Prayer
```

### 구현 예시

#### Domain Event (내부용)

```kotlin
// member/domain/event/MemberRegisteredEvent.kt
data class MemberRegisteredEvent(
    val member: Member              // ✅ 같은 Context의 Domain 객체 OK
) : BaseDomainEvent()
```

#### 같은 Context 이벤트 리스너 (Application Layer)

```kotlin
// member/application/listener/MemberRegisteredEventListener.kt
@Component
class MemberRegisteredEventListener(
    private val emailVerificationTokenPort: EmailVerificationTokenPort,
    private val sendEmailPort: SendEmailPort,
) {
    @EventListener
    fun handle(event: MemberRegisteredEvent) {
        val member = event.member

        // OAuth 가입이면 스킵 (이미 이메일 인증됨)
        if (member.emailVerified) return

        // 이메일 가입이면 인증 메일 발송
        runBlocking {
            val tokenResult = emailVerificationTokenPort.create(member.id)
            sendEmailPort.sendVerificationEmail(
                to = member.email,
                nickname = member.nickname,
                verificationToken = tokenResult.rawToken,
            )
        }
    }
}
```

#### Integration Event (외부용, 필요시)

```kotlin
// member/application/event/MemberCreatedIntegrationEvent.kt
data class MemberCreatedIntegrationEvent(
    val memberId: String,             // ✅ ID만 전달 (Domain 객체 X)
    val nickname: String,
    override val occurredAt: Instant = Instant.now()
) : IntegrationEvent
```

#### 다른 Context 이벤트 핸들러 (Adapter Layer)

```kotlin
// prayer/adapter/inbound/event/MemberCreatedEventHandler.kt
@Component
class MemberCreatedEventHandler(
    private val initializePrayerSettingsUseCase: InitializePrayerSettingsUseCase
) {
    @EventListener
    @Async
    fun handle(event: MemberCreatedIntegrationEvent) {
        // Integration Event 수신 → UseCase 호출
        initializePrayerSettingsUseCase.initialize(
            memberId = MemberId.from(event.memberId)
        )
    }
}
```

### 통신 규칙 요약

#### ✅ 허용

| 항목 | 설명 |
|------|------|
| ID 참조 (Weak Reference) | `memberId: MemberId` |
| Shared Kernel의 Value Object | `Email`, `MemberId` 등 common에 정의된 타입 |
| Integration Event 구독 | Application Layer의 Event DTO |
| 같은 Context 내 Domain Event 구독 | Application Layer의 listener에서 처리 |

#### ❌ 금지

| 항목 | 이유 |
|------|------|
| 다른 Context의 Domain 객체 직접 참조 | Context 경계 침범 |
| 다른 Context의 Repository/Port 직접 주입 | 강한 결합 발생 |
| Integration Event에 Domain 객체 포함 | 외부 Context가 Domain에 의존 |
| 다른 Context 이벤트를 Application Layer에서 처리 | Adapter Layer (`adapter/inbound/event/`)에서 처리해야 함 |

### Selah 이벤트 목록

| Event | 발행 Context | 구독 Context | 용도 |
|-------|-------------|-------------|------|
| `MemberRegisteredEvent` | Member | Member (내부) | 이메일 가입 시 인증 메일 발송 |
| `EmailVerifiedEvent` | Member | Member (내부) | 이메일 인증 완료 처리 |
| `PasswordChangedEvent` | Member | Member (내부) | 비밀번호 변경 알림 (향후) |
| `PrayerAnsweredEvent` | Prayer | Prayer (내부) | 응답된 기도 통계 갱신 (향후) |

---

## E2E 암호화 - Backend 역할

클라이언트에서 암호화된 데이터를 저장/조회하는 역할만 수행합니다. **서버는 평문에 접근할 수 없습니다.**

### Backend 책임

| 책임 | 설명 |
|------|------|
| Salt 저장 | 키 파생용 Salt 저장 (암호화 키 아님) |
| 암호문 CRUD | 암호화된 데이터 저장/조회/수정/삭제 |
| 암호화 설정 관리 | 사용자별 암호화 활성화 상태 관리 |
| 복구 키 해시 저장 | 복구 키 검증용 해시 저장 (복구 키 자체는 저장 금지) |

### 암호화 관련 API

```text
// 암호화 설정 API
POST   /api/v1/encryption/setup      // 암호화 설정 (salt, recoveryKeyHash 저장)
GET    /api/v1/encryption/settings   // 암호화 설정 조회 (salt 반환)
POST   /api/v1/encryption/verify-recovery  // 복구 키 검증
DELETE /api/v1/encryption/settings   // 암호화 설정 삭제 (모든 데이터 삭제됨)
```

### 도메인 모델

```kotlin
// EncryptionSettings - 암호화 설정 (별도 Aggregate)
class EncryptionSettings(
    override val id: EncryptionSettingsId,
    val memberId: MemberId,
    salt: String,                    // Base64 인코딩된 Salt
    recoveryKeyHash: String,         // 복구 키 해시 (검증용)
    isEnabled: Boolean,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    val version: Long?
) : AggregateRoot<EncryptionSettingsId>()
```

### 암호화 필드 처리

기도 데이터의 암호화 필드는 Base64 인코딩된 암호문으로 저장됩니다.

```kotlin
// PrayerTopic - title, reflection은 암호문(Base64)으로 저장
class PrayerTopic(
    // ...
    title: String,           // 암호문 (Base64)
    reflection: String?,     // 암호문 (Base64) 또는 null
    // ...
)

// Prayer - content는 암호문(Base64)으로 저장
class Prayer(
    // ...
    content: String,         // 암호문 (Base64)
    // ...
)
```

### ⚠️ Backend 금지 사항

| 금지 | 이유 |
|------|------|
| 암호화 키 저장/로깅 | E2E 보안 무력화 |
| 평문 검색 기능 구현 | 불가능 (암호문만 저장) |
| 복구 키 원본 저장 | 해시만 저장 가능 |
| Salt를 암호화 키로 오해 | Salt는 키 파생 입력값일 뿐 |

---

## 패키지 구조

```
io.clroot.selah
├── common/                     # 공통 유틸리티, 전역 예외 처리
│   ├── domain/
│   │   ├── AggregateRoot.kt    # Aggregate Root 추상 클래스
│   │   ├── AggregateId.kt      # ID 인터페이스
│   │   └── DomainEntity.kt     # Entity 추상 클래스
│   ├── event/
│   │   ├── DomainEvent.kt      # 도메인 이벤트
│   │   └── IntegrationEvent.kt # 통합 이벤트
│   ├── response/
│   │   ├── ApiResponse.kt      # API 응답 래퍼
│   │   ├── ErrorResponse.kt    # 에러 응답
│   │   └── PageResponse.kt     # 페이지네이션 응답
│   ├── application/
│   │   └── AggregateRootExtensions.kt  # 이벤트 발행 확장 함수
│   ├── security/
│   │   ├── PublicEndpoint.kt           # 공개 API 어노테이션
│   │   └── PublicEndpointRegistry.kt   # 공개 엔드포인트 레지스트리
│   └── util/
│       └── ULIDSupport.kt      # ULID 생성/검증 유틸리티
│
└── domains/
    ├── member/                 # 회원 컨텍스트
    │   ├── adapter/
    │   │   ├── inbound/       # Web Controller, Event Listener
    │   │   └── outbound/      # JPA Repository, External API
    │   ├── application/
    │   │   ├── port/
    │   │   │   ├── inbound/   # UseCase Interfaces
    │   │   │   └── outbound/  # Persistence/Network Port Interfaces
    │   │   └── service/       # UseCase Implementations
    │   └── domain/            # Entities, Value Objects
    │
    └── prayer/                 # 기도 컨텍스트
        ├── adapter/
        │   ├── inbound/
        │   └── outbound/
        ├── application/
        │   ├── port/
        │   └── service/
        └── domain/
```

## Layer별 구현 규칙

### Domain Layer

**핵심 원칙**: 캡슐화(Encapsulation) + 순수 Kotlin + ID 기반 동등성

**AggregateRoot 공통 메타 필드** (생성자로 전달, 부모에서 관리):

| 필드 | 타입 | 변경 가능 | 설명 |
|------|------|----------|------|
| `id` | `ID?` | 불변 | 식별자 |
| `version` | `Long?` | 불변 | 낙관적 락 (JPA @Version) |
| `createdAt` | `LocalDateTime` | 불변 | 생성 시점 |
| `updatedAt` | `LocalDateTime` | `touch()` | 수정 시점 (자식에서 `touch()` 호출) |

#### Aggregate ID 정의 규칙

각 Aggregate/Entity는 전용 ID 타입을 정의합니다.

| ID 전략 | 타입 | 사용 시점 | 장점 |
|---------|------|----------|------|
| **ULID 기반** | `@JvmInline value class XxxId(val value: String) : AggregateId<String>` | 생성 시점에 할당 | DB 저장 전 ID 참조 가능, 분산 환경 친화적 |
| **Long 기반** | `@JvmInline value class XxxId(val value: Long) : AggregateId<Long>` | DB 저장 후 할당 | Auto-increment, 간결함 |

```kotlin
// ✅ ULID 기반 ID (권장)
@JvmInline
value class MemberId(override val value: String) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid MemberId format: $value" }
    }

    companion object {
        fun new(): MemberId = MemberId(ULIDSupport.generateULID())
        fun from(value: String): MemberId = MemberId(value)
    }
}
```

#### 생성자 기본값 정책

**원칙**: Aggregate/Entity 생성자에는 **기본값을 사용하지 않습니다**.

| 구분 | 기본값 사용 | 이유 |
|------|------------|------|
| **생성자** | ❌ 금지 | Adapter에서 Entity → Domain 변환 시 필드 누락을 컴파일 타임에 감지 |
| **Factory 메서드** | ✅ 허용 | 새 객체 생성 시 비즈니스 기본값 적용 |

```kotlin
// ✅ Good: 생성자에 기본값 없음 (모든 값 명시 강제)
// id는 맨 위, 메타 필드(version, createdAt, updatedAt)는 하단에 배치
class PrayerTopic(
    id: PrayerTopicId?,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    title: String,
    status: PrayerStatus,
    answeredAt: LocalDateTime?,
    reflection: String?,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<PrayerTopicId>(id, version, createdAt, updatedAt)

// ✅ Factory 메서드에서 기본값 설정
companion object {
    fun create(memberId: MemberId, title: String): PrayerTopic {
        val now = LocalDateTime.now()
        return PrayerTopic(
            id = PrayerTopicId.new(),
            memberId = memberId,
            title = title,
            status = PrayerStatus.PRAYING,
            answeredAt = null,
            reflection = null,
            version = null,  // 새 객체는 version null
            createdAt = now,
            updatedAt = now,
        )
    }
}
```

#### Aggregate Root (Entity)

```kotlin
// ✅ Good: 캡슐화된 가변성 + AggregateRoot 상속 + 메타 필드는 부모에서 관리
class PrayerTopic(
    id: PrayerTopicId?,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    title: String,
    status: PrayerStatus,
    answeredAt: LocalDateTime?,
    reflection: String?,
    // --- 메타 필드 (하단, 부모에게 전달) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<PrayerTopicId>(id, version, createdAt, updatedAt) {

    // ✅ 비즈니스 필드만 자식에서 관리 (var + private set)
    var title: String = title
        private set
    var status: PrayerStatus = status
        private set
    var answeredAt: LocalDateTime? = answeredAt
        private set
    var reflection: String? = reflection
        private set

    // ✅ 비즈니스 메서드에서 touch() 호출로 updatedAt 갱신
    fun markAsAnswered(reflection: String? = null) {
        status = PrayerStatus.ANSWERED
        answeredAt = LocalDateTime.now()
        this.reflection = reflection
        touch()  // 부모의 updatedAt 갱신
        registerEvent(PrayerAnsweredEvent(this))
    }

    fun updateTitle(newTitle: String) {
        if (title != newTitle) {
            title = newTitle
            touch()  // 부모의 updatedAt 갱신
        }
    }
}

// ❌ Bad: JPA 의존성, public setter
@Entity  // Domain에 JPA 어노테이션 금지!
class PrayerTopic(
    var title: String  // public setter 금지
)
```

**프로퍼티 선언 규칙**:

| 상황 | 패턴 | 예시 |
|------|------|------|
| 단순 타입 (String, Int, LocalDateTime 등) | `var ... private set` | `var title: String = title; private set` |
| 타입 변환 필요 (MutableList → List, MutableMap → Map) | backing field (`_property`) | `private val _items: MutableList<T>; val items: List<T> get() = _items.toList()` |

#### Value Object

```kotlin
// ✅ Value Object는 data class 사용 (불변)
data class Email(val value: String) {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
    }
}
```

**구분 기준**:

| 타입 | 구현 방식 | 동등성 | 이벤트 | 메타 필드 |
|------|----------|--------|--------|----------|
| **Aggregate Root** | `class` + `AggregateRoot<ID>(...)` 상속 | ID 기반 | `registerEvent()` | 부모에게 전달 (id, version, createdAt, updatedAt) |
| **Entity** | `class` | ID 기반 | - | 직접 관리 |
| **Value Object** | `data class` | 모든 필드 | - | 없음 |

### Application Layer

```kotlin
// Input Port (UseCase Interface)
interface CreatePrayerTopicUseCase {
    suspend fun create(command: CreatePrayerTopicCommand): PrayerTopic
}

data class CreatePrayerTopicCommand(
    val memberId: MemberId,
    val title: String
)

// Service (UseCase Implementation)
@Service
@Transactional
class CreatePrayerTopicService(
    private val savePrayerTopicPort: SavePrayerTopicPort
) : CreatePrayerTopicUseCase {

    override suspend fun create(command: CreatePrayerTopicCommand): PrayerTopic {
        val prayerTopic = PrayerTopic.create(
            memberId = command.memberId,
            title = command.title
        )
        return savePrayerTopicPort.save(prayerTopic)
    }
}
```

**규칙**: 비즈니스 로직을 Service에 넣지 말고, Domain 객체에 위임

### Adapter Layer

#### Persistence Adapter

```kotlin
@Component
class PrayerTopicPersistenceAdapter(
    private val repository: PrayerTopicJpaRepository,
    private val mapper: PrayerTopicMapper
) : SavePrayerTopicPort, LoadPrayerTopicPort {

    override suspend fun save(prayerTopic: PrayerTopic): PrayerTopic {
        // ⚠️ JPA는 Blocking! 반드시 Dispatchers.IO 사용
        return withContext(Dispatchers.IO) {
            val entity = mapper.toEntity(prayerTopic)
            val saved = repository.save(entity)
            mapper.toDomain(saved)
        }
    }
}
```

**주의사항**:
- JPA 호출은 반드시 `withContext(Dispatchers.IO)` 내부에서
- `version` 필드를 Entity에 정확히 매핑해야 낙관적 락 동작

#### JDSL 사용 지침

**원칙**: 복잡한 쿼리는 JPQL 문자열 대신 Kotlin JDSL을 사용하여 타입 안전하게 작성합니다.

**JDSL을 사용해야 하는 경우**:
- Fetch Join이 필요한 경우 (Lazy Loading 방지)
- 동적 조건이 포함된 복잡한 쿼리
- 여러 Entity를 조인하는 쿼리

**Spring Data JPA만 사용해도 되는 경우**:
- 단순 CRUD 작업 (`save`, `findById`, `delete`)
- 단순 존재 확인 (`existsById`, `existsByEmail`)
- 메서드 이름 기반 쿼리로 충분한 경우

```kotlin
// ✅ Good: JDSL로 타입 안전한 쿼리 (Fetch Join 포함)
@Component
class MemberPersistenceAdapter(
    private val repository: MemberJpaRepository,
    private val entityManager: EntityManager,
    private val jpqlRenderContext: JpqlRenderContext,
    private val mapper: MemberMapper,
) : LoadMemberPort, SaveMemberPort {

    override suspend fun findById(memberId: MemberId): Member? = withContext(Dispatchers.IO) {
        findMemberEntityBy { path(MemberEntity::id).eq(memberId.value) }
            ?.let { mapper.toDomain(it) }
    }

    override suspend fun existsByEmail(email: Email): Boolean = withContext(Dispatchers.IO) {
        repository.existsByEmail(email.value)  // 단순 쿼리는 JpaRepository 사용
    }

    // Helper: Fetch Join이 포함된 공통 조회 로직
    private fun findMemberEntityBy(predicate: Jpql.() -> Predicate): MemberEntity? {
        val query = jpql {
            selectDistinct(entity(MemberEntity::class))
                .from(
                    entity(MemberEntity::class),
                    leftFetchJoin(MemberEntity::oauthConnections),  // Lazy Loading 방지
                ).where(predicate())
        }
        return entityManager.createQuery(query, jpqlRenderContext)
            .resultList
            .firstOrNull()
    }
}

// ❌ Bad: JPQL 문자열 직접 사용 (타입 안전하지 않음)
@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, String> {
    @Query("SELECT m FROM MemberEntity m LEFT JOIN FETCH m.oauthConnections WHERE m.email = :email")
    fun findByEmailWithConnections(@Param("email") email: String): MemberEntity?
}
```

**JDSL 쿼리 패턴**:

```kotlin
// 기본 조회
val query = jpql {
    select(entity(MemberEntity::class))
        .from(entity(MemberEntity::class))
        .where(path(MemberEntity::email).eq(email))
}

// Fetch Join (1:N 관계)
val query = jpql {
    selectDistinct(entity(MemberEntity::class))
        .from(
            entity(MemberEntity::class),
            leftFetchJoin(MemberEntity::oauthConnections),
        ).where(path(MemberEntity::id).eq(id))
}

// 여러 조건 조합
val query = jpql {
    select(entity(OAuthConnectionEntity::class))
        .from(entity(OAuthConnectionEntity::class))
        .where(
            and(
                path(OAuthConnectionEntity::provider).eq(provider),
                path(OAuthConnectionEntity::providerId).eq(providerId),
            )
        )
}
```

## 코딩 컨벤션

### Naming

| Type | Convention | Example |
|------|------------|---------|
| UseCase Interface | `~UseCase` | `CreatePrayerTopicUseCase` |
| Port Interface | `~Port` | `SavePrayerTopicPort` |
| Service | `~Service` | `CreatePrayerTopicService` |
| Adapter | `~Adapter` | `PrayerTopicPersistenceAdapter` |
| JPA Entity | `~Entity` | `PrayerTopicEntity` |

### Logging

**원칙**: SLF4J를 직접 사용하지 않고 `kotlin-logging`을 사용합니다.

```kotlin
import io.github.oshai.kotlinlogging.KotlinLogging

// ✅ Good: Companion Object + @JvmStatic (권장)
class MyService {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    fun doSomething() {
        logger.info { "Processing..." }
    }
}

// ✅ Good: Lambda 문법 사용 (문자열 연산 지연)
logger.debug { "Processing member: ${member.id}" }
logger.info { "Public endpoint registered: $methods $patterns" }
logger.error(exception) { "Failed to process request" }

// ❌ Bad: 인스턴스 레벨 logger (인스턴스마다 참조 보유)
class MyService {
    private val logger = KotlinLogging.logger {}  // 비효율적
}

// ❌ Bad: SLF4J 직접 사용
private val logger = LoggerFactory.getLogger(MyClass::class.java)
logger.debug("Processing member: {}", member.id)  // 플레이스홀더 문법
```

**Companion Object + @JvmStatic 장점**:
- Java의 static field로 컴파일되어 메모리 효율적
- Logger 이름이 클래스 FQCN과 일치하여 로그 추적 용이
- 인스턴스당 logger lookup 비용 없음

**Lambda 문법 장점**:
- 로그 레벨이 비활성화된 경우 문자열 연산 생략 (성능 최적화)
- Kotlin 스타일의 문자열 템플릿 사용 가능

### Error Handling

```kotlin
// Domain 예외
sealed class DomainException(message: String) : RuntimeException(message)

class PrayerTopicNotFoundException(id: String) : DomainException("PrayerTopic not found: $id")
class MemberNotFoundException(id: String) : DomainException("Member not found: $id")

// Global Handler
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ApiResponse<Nothing>> {
        return when (ex) {
            is PrayerTopicNotFoundException -> ResponseEntity.notFound().body(
                ApiResponse.error(ErrorResponse("PRAYER_TOPIC_NOT_FOUND", ex.message ?: ""))
            )
            is MemberNotFoundException -> ResponseEntity.notFound().body(
                ApiResponse.error(ErrorResponse("MEMBER_NOT_FOUND", ex.message ?: ""))
            )
            else -> ResponseEntity.internalServerError().body(
                ApiResponse.error(ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다"))
            )
        }
    }
}
```

## Common 모듈 사용 가이드

### Response 패키지

API 응답의 일관성을 위한 DTO 클래스들입니다.

```kotlin
// 성공 응답
@GetMapping("/{id}")
suspend fun get(@PathVariable id: String): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
    val topic = getPrayerTopicUseCase.get(id)
    return ResponseEntity.ok(ApiResponse.success(topic.toResponse()))
}

// 에러 응답
return ResponseEntity.badRequest().body(
    ApiResponse.error(ErrorResponse("INVALID_REQUEST", "잘못된 요청입니다"))
)

// 페이지네이션 응답
@GetMapping
suspend fun list(
    @RequestParam page: Int,
    @RequestParam size: Int
): ResponseEntity<ApiResponse<PageResponse<PrayerTopicResponse>>> {
    val result = listPrayerTopicsUseCase.list(page, size)
    return ResponseEntity.ok(ApiResponse.success(
        PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    ))
}
```

### Application 패키지

Aggregate Root의 도메인 이벤트 발행을 위한 확장 함수입니다.

```kotlin
@Service
class CreatePrayerTopicService(
    private val savePrayerTopicPort: SavePrayerTopicPort,
    private val eventPublisher: ApplicationEventPublisher
) : CreatePrayerTopicUseCase {

    @Transactional
    override suspend fun create(command: CreatePrayerTopicCommand): PrayerTopic {
        val prayerTopic = PrayerTopic.create(command.memberId, command.title)
        val saved = savePrayerTopicPort.save(prayerTopic)

        // 도메인 이벤트 발행 및 클리어
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }
}
```

### Security 패키지

인증이 필요 없는 공개 API를 선언적으로 표시합니다.

```kotlin
// 메서드 레벨
@PublicEndpoint
@PostMapping("/login")
suspend fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthTokenResponse>>

// 클래스 레벨 (모든 엔드포인트 공개)
@PublicEndpoint
@RestController
@RequestMapping("/api/v1/auth")
class AuthController { ... }
```

Spring Security 설정에서 사용:

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val publicEndpointRegistry: PublicEndpointRegistry
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(publicEndpointRegistry.getPublicEndpointMatcher()).permitAll()
                    .anyRequest().authenticated()
            }
            .build()
    }
}
```

### Util 패키지 (ULIDSupport)

ULID 생성 및 검증을 위한 유틸리티입니다.

```kotlin
// ID 생성
@JvmInline
value class MemberId(override val value: String) : AggregateId<String> {
    init {
        require(ULIDSupport.isValidULID(value)) { "Invalid MemberId format: $value" }
    }

    companion object {
        fun new(): MemberId = MemberId(ULIDSupport.generateULID())
        fun from(value: String): MemberId = MemberId(value)
    }
}

// ULID 검증
val isValid = ULIDSupport.isValidULID("01ARZ3NDEKTSV4RRFFQ69G5FAV")

// ULID ↔ UUID 변환
val uuid = ULIDSupport.ulidToUUID(ulidString)
val ulid = ULIDSupport.uuidToULID(uuid)
```

## 테스트 규칙

- **Primary Framework**: Kotest (Spec 스타일 준수)
- **Mocking**: MockK
- JUnit 5 혼용 지양

```kotlin
class PrayerTopicServiceTest : BehaviorSpec({
    given("기도제목 생성 시") {
        `when`("유효한 제목이 주어지면") {
            then("새 기도제목이 생성된다") {
                // MockK 활용
            }
        }
    }
})
```

## 빠른 참조 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 특정 컨텍스트 테스트
./gradlew :domains:member:test
./gradlew :domains:prayer:test

# 애플리케이션 실행
./gradlew bootRun
```

## ⚠️ Common Pitfalls (자주 하는 실수)

| 실수 | 올바른 방법 |
|------|-------------|
| Domain에 `@Entity` 붙임 | Adapter Layer의 별도 Entity 클래스 사용 |
| Domain에서 public setter 사용 | `var ... private set` + 비즈니스 메서드로 캡슐화 |
| Aggregate Root를 `data class`로 구현 | `class` + `AggregateRoot<ID>` 상속 |
| 모든 프로퍼티에 backing field 사용 | 단순 타입은 `var ... private set`, 타입 변환 필요 시만 `_property` 사용 |
| Service에 비즈니스 로직 | Domain 객체에 위임 |
| JPA 호출 시 `Dispatchers.IO` 누락 | `withContext(Dispatchers.IO) { }` 감싸기 |
| JPQL 문자열(`@Query`) 직접 사용 | Kotlin JDSL로 타입 안전한 쿼리 작성 |
| suspend 함수에서 `runBlocking` | Coroutine 컨텍스트 전파 활용 |
| Entity ↔ Domain 매핑에서 메타 필드 누락 | id, version, createdAt, updatedAt 모두 매핑 |
| 메타 필드를 자식에서 직접 관리 | 부모(AggregateRoot)에게 생성자로 전달 |
| updatedAt 직접 변경 | `touch()` 메서드 사용 |
| Aggregate/Entity 생성자에 기본값 사용 | 기본값 없이 모든 파라미터 명시, Factory 메서드에서만 기본값 설정 |
| ID를 `Long`으로 직접 사용 | 전용 ID 타입 정의 (`MemberId`, `PrayerTopicId` 등) |
| 암호화 키를 서버에 저장/로깅 | 키는 클라이언트에만 존재해야 함 |
| 암호화 필드를 평문으로 검색 시도 | 암호문(Base64)으로만 저장/조회 |
| SLF4J 직접 사용 (`LoggerFactory.getLogger`) | `kotlin-logging` 사용 (`KotlinLogging.logger {}`) |
| 인스턴스 레벨 logger 정의 | `companion object` + `@JvmStatic`으로 static 정의 |
| Integration Event에 Domain 객체 포함 | Snapshot DTO로 변환하여 전달 |
| 다른 Context의 Domain 직접 import | ID 참조 또는 Integration Event 사용 |
| 다른 Context 이벤트를 Application Layer에서 처리 | Adapter Layer (`adapter/inbound/event/`)에서 처리 |
| 같은 Context 이벤트를 Adapter Layer에서 처리 | Application Layer (`application/listener/`)에서 처리 |

## 코드 생성 시 체크리스트

### Architecture & Domain
- [ ] Domain 클래스가 외부 라이브러리에 의존하지 않는가?
- [ ] Aggregate Root가 `AggregateRoot<ID>(id, version, createdAt, updatedAt)`를 상속하는가?
- [ ] 메타 필드(id, version, createdAt, updatedAt)가 부모에게 전달되는가?
- [ ] 상태 변경이 비즈니스 메서드를 통해서만 이루어지는가? (캡슐화)
- [ ] 비즈니스 메서드에서 상태 변경 시 `touch()`를 호출하는가?
- [ ] 단순 타입은 `var ... private set`, 타입 변환 필요 시만 backing field(`_property`)를 사용하는가?
- [ ] Entity ↔ Domain 매핑이 Mapper를 통해 이루어지는가?
- [ ] Value Object는 `data class`로 구현되었는가?
- [ ] Aggregate/Entity에 전용 ID 타입을 정의하였는가? (`MemberId`, `PrayerTopicId` 등)
- [ ] 생성자에 기본값 없이 모든 파라미터를 명시하는가?
- [ ] 새 객체 생성은 Factory 메서드를 통해 이루어지는가?
- [ ] ULID 기반 ID가 `ULIDSupport`를 사용하여 검증되는가?

### Application Layer
- [ ] 도메인 이벤트 발행 후 `publishAndClearEvents()`를 호출하는가?
- [ ] 같은 Context의 이벤트 리스너가 `application/listener/`에 위치하는가?

### Event & Context 통신
- [ ] Integration Event에 Domain 객체가 아닌 Snapshot DTO를 사용하는가?
- [ ] Domain Event는 `domain/event/`에, Integration Event는 `application/event/`에 위치하는가?
- [ ] 다른 Context의 Domain을 직접 import하지 않는가?
- [ ] 다른 Context의 Event Handler가 `adapter/inbound/event/`에 위치하는가?

### Adapter Layer
- [ ] API 응답이 `ApiResponse`로 감싸져 있는가?
- [ ] 페이지네이션이 `PageResponse`를 사용하는가?
- [ ] 공개 API에 `@PublicEndpoint` 어노테이션이 붙어있는가?
- [ ] 에러 응답이 `ErrorResponse`를 사용하는가?

### Persistence & Concurrency
- [ ] JPA Repository 호출이 `withContext(Dispatchers.IO)` 내부에 있는가?
- [ ] 복잡한 쿼리(Fetch Join, 동적 조건)가 JDSL로 작성되었는가? (JPQL 문자열 금지)
- [ ] 단순 쿼리(existsById, save 등)는 Spring Data JPA 메서드를 활용하는가?

### E2E 암호화 (Backend)
- [ ] 암호화 키를 서버에 저장하거나 로깅하지 않는가?
- [ ] 암호화 필드(title, reflection, content)를 평문으로 다루지 않는가?
- [ ] Salt만 저장하고, 암호화 키는 클라이언트에만 존재하는가?
- [ ] 복구 키 원본이 아닌 해시만 저장하는가?

### Quality
- [ ] 테스트가 Kotest Spec 스타일로 작성되었는가?
- [ ] 로깅이 `kotlin-logging` + `companion object` + `@JvmStatic` 패턴을 사용하는가?
