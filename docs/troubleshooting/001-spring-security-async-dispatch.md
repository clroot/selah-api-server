# Spring Security + Kotlin Coroutines Async Dispatch 문제

이 문서는 Spring MVC 환경에서 Kotlin Coroutines의 `suspend` 함수를 사용할 때 발생하는 Spring Security 인증 전파 문제와 해결 과정을 다룹니다.

## 문제 상황

### 증상
- 클라이언트가 유효한 세션 쿠키를 전송함.
*   인증 필터(`SessionAuthenticationFilter`)에서는 인증에 성공(`Authenticated=true`)한 것으로 로그가 남음.
*   그러나 실제 API 응답은 **401 Unauthorized**를 반환함.

### 원인 분석
Spring MVC의 Controller에서 `suspend` 함수를 사용하면 Spring은 **Async Dispatch** 메커니즘을 사용합니다. 이 과정에서 다음과 같은 문제가 발생합니다:

1.  **첫 번째 Dispatch (REQUEST)**: 필터 체인 실행 → 인증 성공 → Controller 진입.
2.  **Coroutine 일시 중단**: `suspend` 지점에서 비동기 작업 수행을 위해 스레드 반환.
3.  **두 번째 Dispatch (ASYNC)**: 작업 완료 후 결과를 반환하기 위해 새로운 스레드에서 Dispatch 발생.

이때 **ASYNC Dispatch는 새로운 서블릿 요청**으로 취급되어 기존 스레드의 `SecurityContext`가 자동으로 전파되지 않습니다. 결과적으로 두 번째 Dispatch 시 Spring Security는 인증되지 않은 사용자로 판단하여 401 에러를 발생시킵니다.

---

## 해결 방법

### 1. Request Attribute를 통한 Authentication 전파
같은 서블릿 요청 내에서는 `HttpServletRequest` 객체가 유지된다는 점을 활용합니다.

```kotlin
@Component
class SessionAuthenticationFilter(...) : OncePerRequestFilter() {

    // ASYNC Dispatch에서도 필터가 실행되도록 설정
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(...) {
        // ASYNC Dispatch인 경우: 이전에 저장한 인증 정보를 복구
        if (request.dispatcherType == DispatcherType.ASYNC) {
            val savedAuth = request.getAttribute(AUTH_ATTRIBUTE_KEY) as? Authentication
            if (savedAuth != null) {
                SecurityContextHolder.getContext().authentication = savedAuth
            }
            filterChain.doFilter(request, response)
            return
        }

        // 일반 요청(REQUEST): 인증 수행 후 결과를 Attribute에 저장
        // ... 인증 로직 ...
        if (auth != null) {
            request.setAttribute(AUTH_ATTRIBUTE_KEY, auth)
        }
        filterChain.doFilter(request, response)
    }
}
```

### 2. SecurityContextHolder 전략 설정 (보조)
비동기 스레드 간 컨텍스트 전파를 돕기 위해 보안 전략을 변경합니다.

```kotlin
@Configuration
class AsyncSecurityConfig {
    @PostConstruct
    fun init() {
        // 자식 스레드로 SecurityContext 전파 허용
        SecurityContextHolder.setStrategyName(
            SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
        )
    }
}
```

---

## 핵심 요약

| 항목 | 설명 |
|------|------|
| **Async Dispatch** | `suspend` 함수 사용 시 발생하는 Spring MVC의 비동기 처리 방식 |
| **Context 유실** | 새로운 Dispatch 발생 시 ThreadLocal 기반 보안 컨텍스트가 전파되지 않음 |
| **Request Attribute** | Dispatch 간 데이터를 공유하기 위한 안전한 저장소로 활용 |
| **MODE_INHERITABLETHREADLOCAL** | 멀티스레드 환경에서 컨텍스트 공유를 위한 보조 수단 |

---

## 교훈
- **Spring MVC + Coroutines** 조합 시 ThreadLocal 의존성을 항상 경계해야 합니다.
- 프레임워크의 비동기 처리 메커니즘(Async Dispatch)이 보안 필터 체인에 미치는 영향을 이해하는 것이 중요합니다.
- 복잡한 비동기 전파 문제의 경우, 상태를 직접 전파하기보다 요청 객체(`HttpServletRequest`)와 같은 공통 컨텍스트를 활용하는 것이 안정적입니다.
