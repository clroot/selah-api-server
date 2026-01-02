# Spring Data JPA Delete + Kotlin Coroutines 트랜잭션 문제

이 문서는 Kotlin Coroutines의 비동기 환경에서 Spring Data JPA의 `delete` 작업 시 발생하는 트랜잭션 유실 문제와 해결 과정을 다룹니다.

## 문제 상황

### 증상
*   돌아보기(Lookback) 새로고침 기능 수행 시 간헐적으로 서버 에러 발생.
*   API 응답: **500 Internal Server Error**.
*   에러 로그: `jakarta.persistence.TransactionRequiredException: No EntityManager with actual transaction available for current thread`.

### 원인 분석
Spring Data JPA의 `delete` 및 `update` 작업은 **반드시 활성 트랜잭션 내에서 실행**되어야 합니다. 그러나 Coroutines 환경에서는 다음과 같은 이유로 트랜잭션이 유실될 수 있습니다:

1.  **ThreadLocal 기반의 한계**: Spring의 `@Transactional`은 ThreadLocal을 사용하여 트랜잭션 컨텍스트를 관리합니다.
2.  **스레드 전환**: `withContext(Dispatchers.IO)`를 사용하여 I/O 작업을 수행할 때 스레드가 변경되면, 기존 스레드에 묶여 있던 트랜잭션 정보가 전파되지 않습니다.
3.  **EntityManager의 요구사항**: JPA의 `remove()` 호출 시 트랜잭션이 없으면 `TransactionRequiredException`이 발생합니다.

---

## 해결 방법

### Repository 메서드에 @Modifying 및 @Transactional 추가 (권장)
Service 레이어에서의 트랜잭션 전파에 의존하는 대신, Repository 레이어에서 명시적으로 트랜잭션을 관리하도록 설정합니다.

```kotlin
interface LookbackSelectionJpaRepository : JpaRepository<...> {

    @Modifying      // 데이터 변경 작업(DELETE/UPDATE)임을 명시
    @Transactional  // 해당 메서드 호출 시 독자적인 트랜잭션 생성 및 관리
    fun deleteByMemberIdAndSelectedAt(
        memberId: String,
        selectedAt: LocalDate
    )
}
```

이 방식을 사용하면 `suspend` 함수 내에서 스레드가 전환되더라도, Repository 메서드가 호출되는 시점에 Spring Data JPA 프록시가 새로운 트랜잭션을 보장해 줍니다.

---

## 핵심 요약

| 항목 | 설명 |
|------|------|
| **Modifying Query** | SELECT 이외의 데이터 변경 작업은 반드시 `@Modifying` 필요 |
| **Transaction Context** | Coroutines의 `suspend` 지점 사이에서 ThreadLocal 기반 정보는 유실될 수 있음 |
| **Repository Level Tx** | 비동기 환경에서는 데이터 접근 레이어에서 트랜잭션을 직접 보장하는 것이 안전 |

---

## 교훈
- **JPA와 Coroutines의 임피던스 불일치**: JPA는 "Thread-per-request" 모델에 최적화되어 있으므로 비동기 환경 사용 시 항상 주의가 필요합니다.
- **영향 범위의 이해**: 같은 서비스 내의 `SELECT` 쿼리는 트랜잭션 없이도(또는 읽기 전용으로) 동작할 수 있으나, `DELETE/UPDATE`는 훨씬 엄격한 제약 조건을 가집니다.
- **안전한 설계**: 복잡한 트랜잭션 전파 문제를 해결하기 어려울 때는 데이터 변경이 일어나는 가장 말단(Repository)에서 책임을 지도록 설계하는 것이 명확한 해결책이 될 수 있습니다.
