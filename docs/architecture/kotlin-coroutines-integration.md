# Kotlin Coroutines 통합 아키텍처 (Kotlin Coroutines Integration Guide)

이 문서는 Selah 백엔드 아키텍처에서 Kotlin Coroutines을 Spring MVC, Spring Security, 그리고 Spring Data JPA와 결합할 때 발생하는 구조적 문제를 해결하고 안정성을 보장하기 위한 가이드를 다룹니다.

## 아키텍처적 배경 (Architectural Context)

Spring Security와 JPA를 포함한 많은 Java 기반 프레임워크는 **Thread-Bound** 모델을 기반으로 설계되었습니다. 인증 정보와 트랜잭션 상태는 `ThreadLocal`에 저장되며, 이는 "하나의 요청이 하나의 스레드에서 시작부터 끝까지 처리된다"는 가정을 전제로 합니다.

하지만 Kotlin Coroutines은 `suspend` 지점에서 스레드를 자유롭게 반환하고 재개할 수 있는 비동기 모델을 사용하므로, 다음과 같은 **임피던스 불일치(Impedance Mismatch)**가 발생합니다:

1.  **Context 유실**: 스레드가 전환되거나 Spring MVC의 **Async Dispatch**가 발생할 때 `ThreadLocal` 기반의 보안/트랜잭션 정보가 전파되지 않음.
2.  **안정성 위협**: 명시적인 트랜잭션이나 인증 정보가 필요한 작업(예: DELETE 쿼리)에서 런타임 에러 발생.

---

## 아키텍처 결정 (Architectural Decisions)

Selah는 이러한 문제를 해결하고 비동기 환경의 이점을 안전하게 누리기 위해 다음 두 가지 핵심 통합 패턴을 채택했습니다.

### 1. 보안 컨텍스트 전파 (Security Context Propagation)

비동기 Dispatch 과정에서 사용자의 인증 상태가 일관되게 유지되도록 보장합니다.

*   **Request-Scoped 캐싱**: 동일한 서블릿 요청 내에서 공유되는 `HttpServletRequest` Attribute를 인증 정보의 백업 저장소로 활용합니다.
*   **명시적 복구 패턴**: `OncePerRequestFilter` 레벨에서 새로운 Dispatch 발생 시 Attribute에 저장된 정보를 읽어 새로운 스레드의 `SecurityContextHolder`에 다시 채워넣습니다.
*   **전략 보강**: `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL` 설정을 통해 자식 스레드 및 가상 스레드로의 기본적인 상속을 지원합니다.

### 2. 영속성 레이어 트랜잭션 보장 (JPA Transaction Integration)

Coroutines의 스레드 도약(Thread Hopping) 환경에서도 데이터 정합성과 트랜잭션 안정성을 보장합니다.

*   **Repository Level Tx**: 서비스 레이어의 전파에만 의존하지 않고, 데이터 변경이 일어나는 Repository 메서드에 `@Modifying`과 `@Transactional`을 명시하여 독자적인 트랜잭션 경계를 보장합니다.
*   **I/O 격리 (Dispatcher Isolation)**: 모든 JPA 호출(Adapter 레이어)은 반드시 `withContext(Dispatchers.IO)` 블록 내에서 수행하여, 블로킹 작업이 비동기 스케줄링을 방해하지 않도록 격리합니다.

---

## 핵심 원칙 및 레이어별 가이드라인

| 레이어 | 지침 (Guidelines) |
|------|------------------|
| **Controller** | `suspend` 함수를 적극 활용하여 비동기 응답 처리 |
| **Service** | 비즈니스 로직에 집중하되, 전역 트랜잭션 전파 유실 가능성 염두 |
| **Adapter** | 모든 JPA 호출을 `Dispatchers.IO`로 격리하여 실행 |
| **Repository** | `DELETE`, `UPDATE` 작업 시 반드시 `@Modifying` 및 `@Transactional` 적용 |

---

## 기술적 요약 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                   Servlet Request Lifecycle                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [Filter] -> Save Auth to Request Attribute                 │
│      │                                                      │
│      ▼                                                      │
│  [Controller] -> suspend fun start (Thread A)               │
│      │                                                      │
│      ▼ (Suspension Point - Thread Released)                 │
│                                                             │
│  [Service/Adapter] -> withContext(Dispatchers.IO)           │
│      │               (Thread B - Potential Hopping)          │
│      │                                                      │
│      └─▶ [Repository] -> @Transactional (Ensure New Tx)     │
│                                                             │
│      ▲ (Resume - New Async Dispatch)                        │
│      │                                                      │
│  [Filter] -> Restore Auth from Request Attribute            │
│      │                                                      │
│      ▼                                                      │
│  [Controller] -> Send Response (Thread C)                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 결론
이러한 아키텍처 패턴을 통해 Selah는 JPA의 성숙한 생태계를 그대로 활용하면서도, Kotlin Coroutines의 비동기적 효율성을 안정적으로 확보하고 있습니다. 개발자는 스레드나 컨텍스트 유실에 대한 고민 없이 비즈니스 로직 구현에만 집중할 수 있습니다.
