package io.clroot.selah.common.security

/**
 * 인증 없이 접근 가능한 공개 API를 표시하는 어노테이션
 *
 * 컨트롤러 메서드에 이 어노테이션을 붙이면 해당 엔드포인트는
 * 인증 없이 접근할 수 있습니다.
 *
 * ```kotlin
 * @PublicEndpoint
 * @PostMapping("/login")
 * suspend fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthTokenResponse>
 * ```
 *
 * 클래스 레벨에 붙이면 해당 컨트롤러의 모든 엔드포인트가 공개됩니다.
 *
 * ```kotlin
 * @PublicEndpoint
 * @RestController
 * @RequestMapping("/api/public")
 * class PublicController { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PublicEndpoint
