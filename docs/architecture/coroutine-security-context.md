# 비동기 환경에서의 보안 컨텍스트 전파 (Security Context Propagation in Coroutines)

이 문서는 Spring MVC 환경에서 Kotlin Coroutines을 사용할 때, 서로 다른 스레드 간에 보안 컨텍스트(SecurityContext)를 안정적으로 공유하고 유지하기 위한 아키텍처 결정을 다룹니다.

## 아키텍처적 배경 (Architectural Context)

Spring Security는 기본적으로 `ThreadLocal`을 사용하여 인증 정보(`Authentication`)를 관리합니다. 이는 "요청당 하나의 스레드"가 할당되는 전통적인 블로킹 모델에 최적화되어 있습니다. 

반면, Kotlin Coroutines은 `suspend` 지점에서 스레드를 반환하고, 재개 시 다른 스레드에서 실행될 수 있습니다. 특히 Spring MVC의 Coroutine 지원 방식인 **Async Dispatch**는 첫 번째 인증 스레드와 비동기 작업 후 응답을 처리하는 스레드가 분리되어 `ThreadLocal` 기반의 보안 정보가 유실되는 구조적 임피던스 불일치(Impedance Mismatch)를 야기합니다.

---

## 아키텍처 결정 (Architectural Decisions)

Selah는 이 문제를 해결하기 위해 다음 두 가지 패턴을 표준 아키텍처로 채택했습니다.

### 1. Request-Scoped 상태 전파 (Primary)
스레드는 변경되더라도 동일한 서블릿 요청 내에서는 `HttpServletRequest` 객체가 공유된다는 점을 활용합니다.

*   **패턴**: `OncePerRequestFilter`에서 인증 성공 시 그 결과를 `HttpServletRequest`의 Attribute에 캐싱합니다.
*   **복구 로직**: 프레임워크에 의해 새로운 Dispatch가 발생할 때, 캐싱된 인증 정보를 읽어 새로운 스레드의 `SecurityContextHolder`에 다시 채워넣습니다.
*   **이점**: 스레드 전환이나 Dispatch 방식에 관계없이 요청의 생명주기 동안 인증 정보의 일관성을 완벽하게 보장합니다.

### 2. 가상 스레드 친화적 전략 (Secondary)
JDK 21의 가상 스레드(Virtual Threads) 환경을 대비하여 스레드 로컬 전파 전략을 보강합니다.

*   **설정**: `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL`을 기본 전략으로 사용합니다.
*   **이점**: 부모 스레드에서 생성된 자식 스레드(또는 가상 스레드)로 보안 컨텍스트가 자연스럽게 상속되도록 돕습니다.

---

## 핵심 원칙 (Core Principles)

| 원칙 | 설명 |
|------|------|
| **Thread Independence** | 비즈니스 로직은 특정 스레드 로컬 상태에 의존하지 않아야 함 |
| **Request Consistency** | 비동기 Dispatch 과정에서도 사용자의 인증 상태는 요청 단위로 일관되어야 함 |
| **Explicit Recovery** | 프레임워크의 암시적 동작에 의존하기보다 필터 레벨에서 명시적으로 컨텍스트를 복구함 |

---

## 결론 및 가이드라인
비동기 컨트롤러(`suspend fun`)를 작성할 때 개발자는 스레드 유실을 걱정할 필요가 없습니다. 시스템 아키텍처 수준에서 요청 속성(Request Attribute)을 통한 인증 정보 복원 메커니즘이 투명하게 작동하여, 보안 기반 서비스 로직을 안전하게 수행할 수 있습니다.
