package io.clroot.selah.common.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder

/**
 * 비동기 요청에서 SecurityContext를 전파하기 위한 설정
 *
 * Kotlin suspend 함수를 사용하는 Controller에서
 * SecurityContext가 비동기 스레드로 전파되도록 합니다.
 */
@Configuration
class AsyncSecurityConfig {
    @PostConstruct
    fun init() {
        // Virtual Threads와 함께 사용하기 위해 MODE_INHERITABLETHREADLOCAL 설정
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
    }
}
