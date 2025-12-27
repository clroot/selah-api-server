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
| Security | Spring Security | OAuth2 + JWT |
| Async | Kotlin Coroutines, Virtual Threads | 비동기 처리 |
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

**Aggregates**: `Member`(Root)

### 🙏 2. Prayer Context (기도 도메인)

**역할**: "기도 관리자"

| 책임 | 설명 |
|------|------|
| 기도제목 관리 | 기도제목 CRUD, 응답 체크 |
| 기도문 관리 | 기도문 작성, 조회 |
| 돌아보기 | 과거 기도제목 선정 및 알림 |
| 통계 | 응답된 기도 통계 제공 |

**Aggregates**: `PrayerTopic`(Root), `Prayer`(Root)

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
class PrayerTopic(
    override val id: PrayerTopicId?,
    memberId: MemberId,
    title: String,
    status: PrayerStatus,
    answeredAt: LocalDateTime?,
    reflection: String?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    val version: Long?
) : AggregateRoot<PrayerTopicId>()

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
            createdAt = now,
            updatedAt = now,
            version = null
        )
    }
}
```

#### Aggregate Root (Entity)

```kotlin
// ✅ Good: 캡슐화된 가변성 + AggregateRoot 상속 + 기본값 없음
class PrayerTopic(
    override val id: PrayerTopicId?,
    memberId: MemberId,
    title: String,
    status: PrayerStatus,
    answeredAt: LocalDateTime?,
    reflection: String?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    val version: Long?
) : AggregateRoot<PrayerTopicId>() {

    // ✅ 단순 타입: var + private set (간결함)
    val memberId: MemberId = memberId
    var title: String = title
        private set
    var status: PrayerStatus = status
        private set
    var answeredAt: LocalDateTime? = answeredAt
        private set
    var reflection: String? = reflection
        private set
    var createdAt: LocalDateTime = createdAt
        private set
    var updatedAt: LocalDateTime = updatedAt
        private set

    // 비즈니스 메서드를 통해서만 상태 변경 + 이벤트 등록
    fun markAsAnswered(reflection: String? = null) {
        status = PrayerStatus.ANSWERED
        answeredAt = LocalDateTime.now()
        this.reflection = reflection
        updatedAt = LocalDateTime.now()
        registerEvent(PrayerAnsweredEvent(this))
    }

    fun updateTitle(newTitle: String) {
        if (title != newTitle) {
            title = newTitle
            updatedAt = LocalDateTime.now()
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

| 타입 | 구현 방식 | 동등성 | 이벤트 |
|------|----------|--------|--------|
| **Aggregate Root** | `class` + `AggregateRoot<ID>` 상속 | ID 기반 | `registerEvent()` |
| **Entity** | `class` | ID 기반 | - |
| **Value Object** | `data class` | 모든 필드 | - |

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

## 코딩 컨벤션

### Naming

| Type | Convention | Example |
|------|------------|---------|
| UseCase Interface | `~UseCase` | `CreatePrayerTopicUseCase` |
| Port Interface | `~Port` | `SavePrayerTopicPort` |
| Service | `~Service` | `CreatePrayerTopicService` |
| Adapter | `~Adapter` | `PrayerTopicPersistenceAdapter` |
| JPA Entity | `~Entity` | `PrayerTopicEntity` |

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
| suspend 함수에서 `runBlocking` | Coroutine 컨텍스트 전파 활용 |
| Entity `version` 매핑 누락 | Mapper에서 반드시 version 포함 |
| Aggregate/Entity 생성자에 기본값 사용 | 기본값 없이 모든 파라미터 명시, Factory 메서드에서만 기본값 설정 |
| ID를 `Long`으로 직접 사용 | 전용 ID 타입 정의 (`MemberId`, `PrayerTopicId` 등) |

## 코드 생성 시 체크리스트

### Architecture & Domain
- [ ] Domain 클래스가 외부 라이브러리에 의존하지 않는가?
- [ ] Aggregate Root가 `AggregateRoot<ID>`를 상속하는가?
- [ ] 상태 변경이 비즈니스 메서드를 통해서만 이루어지는가? (캡슐화)
- [ ] 단순 타입은 `var ... private set`, 타입 변환 필요 시만 backing field(`_property`)를 사용하는가?
- [ ] Domain 클래스에 `version` 필드가 있는가?
- [ ] Entity ↔ Domain 매핑이 Mapper를 통해 이루어지는가?
- [ ] Value Object는 `data class`로 구현되었는가?
- [ ] Aggregate/Entity에 전용 ID 타입을 정의하였는가? (`MemberId`, `PrayerTopicId` 등)
- [ ] 생성자에 기본값 없이 모든 파라미터를 명시하는가?
- [ ] 새 객체 생성은 Factory 메서드를 통해 이루어지는가?
- [ ] ULID 기반 ID가 `ULIDSupport`를 사용하여 검증되는가?

### Application Layer
- [ ] 도메인 이벤트 발행 후 `publishAndClearEvents()`를 호출하는가?

### Adapter Layer
- [ ] API 응답이 `ApiResponse`로 감싸져 있는가?
- [ ] 페이지네이션이 `PageResponse`를 사용하는가?
- [ ] 공개 API에 `@PublicEndpoint` 어노테이션이 붙어있는가?
- [ ] 에러 응답이 `ErrorResponse`를 사용하는가?

### Persistence & Concurrency
- [ ] JPA Repository 호출이 `withContext(Dispatchers.IO)` 내부에 있는가?

### Quality
- [ ] 테스트가 Kotest Spec 스타일로 작성되었는가?
