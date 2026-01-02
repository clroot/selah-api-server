# Coroutines와 JPA의 안정적인 통합 (Coroutines & JPA Integration)

이 문서는 비동기 실행 환경(Kotlin Coroutines)에서 블로킹 영속성 프레임워크(Spring Data JPA)를 사용할 때 발생하는 데이터 정합성 및 트랜잭션 유실 문제를 방지하기 위한 아키텍처 가이드를 제시합니다.

## 아키텍처적 도전 과제 (Challenges)

JPA(Java Persistence API)는 본질적으로 **Thread-Bound** 모델입니다. `EntityManager`와 트랜잭션 상태는 `ThreadLocal`에 묶여 있으며, 이는 하나의 요청이 하나의 스레드에서 끝까지 처리된다는 가정하에 설계되었습니다.

하지만 Kotlin Coroutines 기반의 비동기 환경에서는 다음과 같은 문제로 인해 트랜잭션 컨텍스트가 유실될 수 있습니다:
1.  **스레드 도약(Thread Hopping)**: `withContext(Dispatchers.IO)`와 같은 호출로 스레드가 전환될 때 `ThreadLocal` 정보가 전파되지 않음.
2.  **임피던스 불일치**: `suspend` 지점에서 트랜잭션 경계가 모호해져, 특히 `DELETE`나 `UPDATE`와 같이 명시적인 트랜잭션을 요구하는 작업에서 에러 발생.

---

## 아키텍처 결정 (Architectural Decisions)

Selah는 영속성 레이어의 안정성을 위해 다음 패턴을 아키텍처 표준으로 정의합니다.

### 1. Repository Level 트랜잭션 보장
Service 레이어에서의 트랜잭션 전파(`@Transactional`)에만 의존하지 않고, 데이터 변경이 일어나는 가장 말단인 Repository 레이어에서 트랜잭션을 스스로 보장하도록 설계합니다.

*   **패턴**: 데이터 변경 작업(`deleteBy...`, `update...`) 수행 시 Repository 메서드에 `@Modifying`과 `@Transactional`을 함께 명시합니다.
*   **근거**: `suspend` 함수 내에서 어떤 스레드 전환이 일어나더라도, Repository 메서드가 호출되는 시점에 Spring Data JPA 프록시가 새로운 트랜잭션을 확실히 시작하도록 강제하여 `TransactionRequiredException`을 원천 차단합니다.

### 2. IO Dispatcher 활용 및 고립
JPA의 블로킹 작업을 Coroutine의 메인 실행 흐름에서 분리하기 위해 전용 디스패처를 사용합니다.

*   **원칙**: 모든 JPA 호출(Adapter 레이어)은 반드시 `withContext(Dispatchers.IO)` 블록 내에서 수행됩니다.
*   **이점**: 블로킹 I/O 작업이 애플리케이션의 비동기 스케줄링 흐름을 방해하지 않도록 격리합니다.

---

## 개발 원칙 (Core Principles)

| 원칙 | 설명 |
|------|------|
| **Self-Contained Persistence** | 영속성 작업은 호출 환경의 상태와 관계없이 스스로 실행 가능해야 함 |
| **Explicit Modifying** | 모든 데이터 변경 쿼리는 `@Modifying`을 통해 명시적으로 관리함 |
| **Context Isolation** | 블로킹 라이브러리(JPA)와 비동기 라이브러리(Coroutines)의 경계를 명확히 분리함 |

---

## 기술적 대안 및 향후 방향
장기적으로는 Coroutines와 네이티브하게 호환되는 **Spring Data R2DBC**와 같은 리액티브 스택으로의 전환이 근본적인 해결책이 될 수 있습니다. 하지만 현재는 JPA의 방대한 생태계와 성숙도를 활용하면서도 위와 같은 아키텍처 패턴을 통해 비동기 환경에서의 안정성을 확보하고 있습니다.
