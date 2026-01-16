package io.clroot.selah.domains.member.adapter.inbound.security

import io.clroot.selah.common.security.PublicEndpointRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
 *
 * 세션 기반 인증 + API Key 인증을 지원합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val sessionAuthFilter: SessionAuthenticationFilter,
    private val apiKeyAuthFilter: ApiKeyAuthenticationFilter,
    private val publicEndpointRegistry: PublicEndpointRegistry,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() } // REST API
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement {
                // Spring Security의 세션 관리 비활성화 (직접 관리)
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests { auth ->
                auth
                    // Actuator health 엔드포인트 허용 (배포 스크립트 헬스체크용)
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    // 공개 엔드포인트 허용
                    .requestMatchers(publicEndpointRegistry.getPublicEndpointMatcher())
                    .permitAll()
                    // 나머지는 인증 필요
                    .anyRequest()
                    .authenticated()
            }
            // API Key 필터가 먼저 실행 (X-API-Key 헤더 우선)
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            // 세션 필터가 API Key 필터 다음에 실행
            .addFilterBefore(sessionAuthFilter, ApiKeyAuthenticationFilter::class.java)
            .exceptionHandling {
                // 인증 실패 시 401 반환
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }.build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                // TODO: 실제 배포 시 허용 도메인 설정
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
                exposedHeaders = listOf("Set-Cookie")
                maxAge = 3600L
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
