# Kotlin Coroutines와 Spring의 불편한 동거

> Kotlin Coroutines을 Spring MVC, Spring Security, Spring Data JPA와 함께 사용하면서 겪은 문제들과 해결 과정을 정리한 문서입니다.

---

### 요약

| 항목 | 내용 |
|-----|-----|
| **목표** | Reactive 전환 대비 + 비동기 구조 선행 학습 |
| **문제** | Spring의 ThreadLocal 기반 설계와 Coroutines 충돌 |
| **해결** | SecurityContext 수동 전파, Repository 단위 트랜잭션, `Dispatchers.IO` 격리 |
| **한계** | 복합 트랜잭션 원자성 미보장 (Saga 패턴 또는 보상 트랜잭션 필요) |
| **로드맵** | Hibernate Reactive 전환 (조건부) |

---

## 왜 Coroutines인가?

### 리액티브 전환의 징검다리

Selah는 개인 프로젝트이므로 새로운 기술을 시도하기 좋은 환경이었습니다. Coroutines을 선택한 이유는 **향후 Reactive Stack 전환을 염두에 두었기 때문**입니다.

전통적인 Blocking 코드에서 Reactive(Mono/Flux)로 직접 전환하면 코드베이스 전체를 수정해야 합니다:

```kotlin
// Blocking → Reactive: 반환 타입 변경, 모든 호출부 수정 필요
fun findById(id: String): Entity?
    ↓
fun findById(id: String): Mono<Entity>
```

하지만 Coroutines를 거치면 전환 비용이 줄어듭니다:

```kotlin
// Blocking → Coroutines: suspend 키워드 추가
suspend fun findById(id: String): Entity?

// Coroutines → Reactive: 내부 구현만 변경
sessionFactory.withSession { ... }.awaitSuspending()
```

함수 시그니처가 이미 비동기적이므로, 나중에 Hibernate Reactive로 전환해도 **내부 구현만 바꾸면 됩니다.**

---

## 마주친 문제들

### 문제 1: 인증 성공 후 401 Unauthorized

세션 기반 인증이 간헐적으로 실패하는 버그가 발생했습니다.

```
[tomcat-handler-9] SessionFilter: Authenticated=true  ✅
[tomcat-handler-9] Secured GET /api/v1/members/me

[tomcat-handler-10] Securing GET /api/v1/members/me  ⚠️
[tomcat-handler-10] AnonymousAuthenticationFilter: Set to anonymous
[tomcat-handler-10] Response: 401 Unauthorized  ❌
```

curl로 **하나의 요청**만 보냈는데, 서버 로그에는 **두 개의 요청**이 찍혔습니다.

### 문제 2: SELECT는 성공, DELETE만 실패

```kotlin
@Transactional
class LookbackService(...) {
    override suspend fun refresh(memberId: MemberId): LookbackResult {
        deleteLookbackSelectionPort.deleteByMemberIdAndDate(memberId, today)  // ❌
        return selectAndSave(memberId, today)
    }
}
```

```
No EntityManager with actual transaction available for current thread
```

같은 Adapter에서 SELECT는 정상 동작하는데, DELETE만 실패했습니다.

---

## 근본 원인: Thread-Bound vs Coroutines

두 문제의 공통 원인은 **임피던스 불일치(Impedance Mismatch)**입니다. 서로 다른 설계 철학을 가진 기술을 함께 사용할 때 발생하는 구조적 충돌을 말합니다.

### Spring의 가정

Spring Security와 JPA는 **"하나의 요청 = 하나의 스레드"**를 가정합니다. 인증 정보, 트랜잭션 상태 등을 **ThreadLocal**에 저장하는데, ThreadLocal은 스레드별로 독립된 저장소를 제공하는 Java의 메커니즘입니다.

```
┌─────────────────────────────────────────────────────────────┐
│  Traditional Request Lifecycle                              │
├─────────────────────────────────────────────────────────────┤
│  [Request] ─────── Single Thread (A) ──────► [Response]     │
│                                                             │
│  ThreadLocal: SecurityContext, EntityManager, Transaction   │
└─────────────────────────────────────────────────────────────┘
```

### Coroutines의 현실

`suspend` 함수는 suspension point(I/O 대기, delay, 다른 suspend 함수 호출 등 실행이 일시 중단되는 지점)에서 스레드를 반환하고, 재개 시 **다른 스레드**에서 실행될 수 있습니다.

```
┌─────────────────────────────────────────────────────────────┐
│  Coroutines Request Lifecycle                               │
├─────────────────────────────────────────────────────────────┤
│  Thread A: Filter → Controller starts                       │
│       ▼ (suspend)                                           │
│  Thread B: Service/Adapter executes                         │
│       ▼ (suspend)                                           │
│  Thread C: Controller resumes → Response                    │
│                                                             │
│  ThreadLocal on Thread C: Empty! ❌                         │
└─────────────────────────────────────────────────────────────┘
```

**문제 1**: Spring MVC는 `suspend` Controller를 처리할 때 **Async Dispatch**를 사용합니다. Async Dispatch란 하나의 HTTP 요청을 여러 단계로 나눠 처리하는 서블릿 메커니즘입니다. 첫 번째 dispatch에서 인증이 완료되었지만, 두 번째 dispatch는 새로운 스레드에서 실행되어 ThreadLocal의 `SecurityContext`가 비어있었습니다.

**문제 2**: Service의 `@Transactional`은 Thread A에서 시작되었지만, `withContext(Dispatchers.IO)` 이후 Thread B에서 Repository가 호출되었습니다. ThreadLocal에 저장된 트랜잭션 컨텍스트가 Thread B로 전파되지 않았습니다. SELECT는 트랜잭션 없이도 동작하지만, DELETE는 활성 트랜잭션이 필요합니다.

---

## JPA를 유지한 이유

### R2DBC라는 대안

근본적인 해결책은 Reactive Stack으로 전환하는 것입니다.

```kotlin
// R2DBC + Coroutines는 완벽하게 호환
interface PrayerTopicRepository : CoroutineCrudRepository<PrayerTopicEntity, String> {
    suspend fun deleteByMemberId(memberId: String): Long  // Native suspend
}
```

### 그럼에도 JPA를 선택한 이유

| 고려 사항               | JPA                        | R2DBC    |
|---------------------|----------------------------|----------|
| 생태계 성숙도             | 20년+                       | 2018년 출시 |
| ORM 기능              | 완전한 ORM                    | 단순 매핑    |
| **Type-safe Query** | **JDSL, QueryDSL**         | 제한적      |
| 복잡한 관계              | `@OneToMany`, Lazy Loading | 수동 Join  |

**결정적 이유는 JDSL입니다.** Type-safe한 쿼리를 작성하고 싶었고, R2DBC 생태계에서는 이를 만족시키는 도구가 부족했습니다.

```kotlin
// JDSL - 컴파일 타임에 쿼리 오류 검출
val query = jpql {
    select(entity(PrayerTopicEntity::class))
        .from(entity(PrayerTopicEntity::class))
        .where(path(PrayerTopicEntity::memberId).eq(memberId))
}
```

추가로:

- Selah는 개인용 서비스로 대규모 동시성이 필요하지 않음
- Virtual Threads (JDK 21)로 Blocking I/O도 효율적으로 처리 가능
- 마이그레이션 비용 대비 이득이 크지 않음

---

## 해결 패턴

### 1. Security Context 전파

`HttpServletRequest`의 Attribute는 Async Dispatch 간에도 공유됩니다.

```kotlin
@Component
class SessionAuthenticationFilter(...) : OncePerRequestFilter() {

    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(request: HttpServletRequest, ...) {
        // ASYNC Dispatch: Attribute에서 복구
        if (request.dispatcherType == DispatcherType.ASYNC) {
            val savedAuth = request.getAttribute(AUTH_KEY) as? Authentication
            savedAuth?.let { SecurityContextHolder.getContext().authentication = it }
            filterChain.doFilter(request, response)
            return
        }

        // 일반 요청: 인증 후 Attribute에 저장
        authenticateWithSession(sessionToken, ipAddress)
        SecurityContextHolder.getContext().authentication?.let {
            request.setAttribute(AUTH_KEY, it)
        }
        filterChain.doFilter(request, response)
    }
}
```

### 2. Repository 레벨 트랜잭션

Service의 `@Transactional`에 의존하지 않고, Repository 메서드에 직접 트랜잭션을 명시합니다.

```kotlin
interface LookbackSelectionJpaRepository : JpaRepository<LookbackSelectionEntity, String> {
    @Modifying
    @Transactional
    fun deleteByMemberIdAndSelectedAt(memberId: String, selectedAt: LocalDate)
}
```

**규칙:**

- 커스텀 DELETE/UPDATE 메서드: `@Modifying` + `@Transactional` 필수
- `JpaRepository` 기본 메서드(`deleteById`, `save`): 이미 `@Transactional` 적용됨

### 3. I/O 격리

JPA는 Blocking I/O입니다. Coroutines의 기본 스케줄러(`Dispatchers.Default`)는 CPU 바운드 작업에 최적화되어 스레드 수가 제한적입니다. Blocking I/O를 기본 스케줄러에서 실행하면 스레드 풀이 고갈될 수 있으므로, 모든 JPA 호출은 `withContext(Dispatchers.IO)` 내에서 수행합니다.

```kotlin
override suspend fun deleteById(id: PrayerId) =
    withContext(Dispatchers.IO) {
        repository.deleteById(id.value)
    }
```

---

## 검증: Testcontainers

스레드 전환으로 인한 트랜잭션 유실 문제는 Mock 테스트로 재현되지 않습니다. Coroutines 환경에서 트랜잭션이 실제로 커밋되는지 검증하려면 실제 데이터베이스가 필요합니다.

```kotlin
// 싱글톤 PostgreSQL 컨테이너
object PostgreSQLTestContainer {
    val instance: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .apply { withReuse(true) }
    }
}

// 테스트 트랜잭션 비활성화 - UseCase의 트랜잭션이 실제로 커밋되는지 검증
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class IntegrationTestBase : DescribeSpec() {
    init {
        extension(DatabaseTestExtension())
        extension(SpringExtension())
    }
}
```

```kotlin
@SpringBootTest
class PrayerPersistenceAdapterIntegrationTest : IntegrationTestBase() {
    init {
        describe("deleteById") {
            it("Prayer가 삭제된다") {
                val prayer = Prayer.create(memberId, emptyList(), "content")
                adapter.save(prayer)

                adapter.deleteById(prayer.id)

                adapter.findById(prayer.id).shouldBeNull()
            }
        }
    }
}
```

### 커버리지 강제

Persistence Adapter에 대해 JaCoCo로 80% 커버리지를 강제합니다. Mock 테스트로는 커버리지는 달성할 수 있지만 실제 트랜잭션 동작을 검증할 수 없으므로, 자연스럽게 Testcontainers를 사용한 통합 테스트를 작성하게 됩니다.

```kotlin
// build.gradle.kts
jacocoTestCoverageVerification {
    violationRules {
        // Persistence Adapter는 실제 DB 테스트 필수
        rule {
            element = "CLASS"
            includes = listOf("*.*PersistenceAdapter")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
```

커버리지 미달 시 `./gradlew check`가 실패합니다.

---

## 알려진 한계

### 복합 작업의 원자성

여러 Repository 호출을 하나의 트랜잭션으로 묶는 것은 보장되지 않습니다.

```kotlin
@Transactional
class LookbackService(...) {
    override suspend fun refresh(memberId: MemberId): LookbackResult {
        deleteLookbackSelectionPort.deleteByMemberIdAndDate(memberId, today)  // 커밋됨
        return selectAndSave(memberId, today)  // 여기서 예외 발생 시 삭제는 롤백 안됨
    }
}
```

현재 대응:

- 이 한계를 인지하고, 필요 시 도메인 로직으로 보상 처리
- Selah의 사용 패턴상 드문 케이스로 수용 가능한 trade-off

---

## 레이어별 가이드라인

| Layer          | Guideline                                             |
|----------------|-------------------------------------------------------|
| **Controller** | `suspend fun` 사용                                      |
| **Service**    | `@Transactional`은 suspend에서 유실 가능성 인지                 |
| **Adapter**    | 모든 JPA 호출은 `withContext(Dispatchers.IO)` 내에서          |
| **Repository** | 커스텀 DELETE/UPDATE에 `@Modifying` + `@Transactional` 필수 |

---

## 향후 계획: Hibernate Reactive

현재 해결 패턴들은 임피던스 불일치를 **우회**하는 것이지 **해결**하는 것이 아닙니다. 근본적인 해결을 위해 Hibernate Reactive 전환을 검토 중입니다.

### 왜 R2DBC가 아닌 Hibernate Reactive인가?

| 항목             | R2DBC       | Hibernate Reactive             |
|----------------|-------------|--------------------------------|
| **JDSL 지원**    | ❌ 불가        | ✅ `hibernate-reactive-support` |
| JPA Entity 재사용 | ❌ 별도 Entity | ✅ 그대로 사용                       |
| ORM 기능         | 단순 매핑       | 완전한 ORM                        |
| Spring 통합      | 공식 지원       | 수동 구성 필요                       |

**JDSL을 유지할 수 있다는 점이 결정적입니다.**

```kotlin
// Hibernate Reactive + JDSL - 기존 쿼리 거의 그대로 사용
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery

suspend fun findById(id: PrayerTopicId): PrayerTopic? =
    sessionFactory.withSession { session ->
        val query = jpql {
            select(entity(PrayerTopicEntity::class))
                .from(entity(PrayerTopicEntity::class))
                .where(path(PrayerTopicEntity::id).eq(id.value))
        }
        session.createQuery(query, jpqlRenderContext).singleResultOrNull()
    }.awaitSuspending()
```

### Hibernate Reactive의 장점

| 장점                  | 설명                               |
|---------------------|----------------------------------|
| 네이티브 Non-blocking   | Vert.x 기반 Reactive Driver 사용     |
| Coroutines 자연스러운 통합 | `Mutiny.awaitSuspending()`       |
| 트랜잭션 문제 해결          | `withTransaction {}` 블록으로 명시적 관리 |
| `withContext` 불필요   | Dispatcher 전환 workaround 제거      |
| Entity 재사용          | 기존 JPA Entity 그대로 사용             |

### 전환 보류 이유

- Spring Boot에서 공식 지원하지 않음 (수동 구성 필요)
- 마이그레이션 비용 (약 22-24시간)
- 현재 Selah는 개인용 서비스로 대규모 트래픽이 없음
- Virtual Threads로 현재 성능 충분

### 전환 조건

다음 중 하나가 충족되면 마이그레이션 재검토:

- Spring Boot에서 Hibernate Reactive 공식 지원 시
- 동시 사용자 1,000명 이상 예상 시
- 현재 workaround로 해결 불가능한 문제 발생 시

---

## 결론

Kotlin Coroutines과 Spring JPA 사이에는 근본적인 임피던스 불일치가 존재합니다. ThreadLocal 기반 프레임워크와 스레드 도약이 자유로운 Coroutines은 설계 철학이 다릅니다.

이 문서에서 다룬 해결 패턴들은 이 불일치를 **우회**하는 방법입니다:

1. Security Context를 Request Attribute로 백업/복구
2. Repository 레벨에서 트랜잭션 경계 명시
3. Testcontainers로 실제 DB 동작 검증

이 접근으로 JPA 생태계(특히 JDSL)를 유지하면서 Coroutines의 비동기 효율성을 대부분의 경우 안전하게 활용하고 있습니다. 향후 Hibernate Reactive가 Spring에서 공식 지원되면 근본적인 해결을 위해 전환을 검토할 예정입니다.

---

## 참고 자료

- [Spring Security - Servlet Async Support](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html#mvc-async)
- [Spring Data JPA - Modifying Queries](https://docs.spring.io/spring-data/jpa/reference/jpa/modifying-queries.html)
- [Kotlin JDSL](https://kotlin-jdsl.gitbook.io/docs)
- [Hibernate Reactive](https://hibernate.org/reactive/)
- [JDSL Hibernate Reactive Support](https://kotlin-jdsl.gitbook.io/docs/jpql-with-kotlin-jdsl/hibernate-reactive-supports)
