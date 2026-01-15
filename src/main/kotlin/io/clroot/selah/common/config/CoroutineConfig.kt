package io.clroot.selah.common.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 애플리케이션 레벨 코루틴 스코프 설정
 *
 * 이벤트 리스너 등에서 비동기 작업을 수행할 때 사용합니다.
 * SupervisorJob을 사용하여 하나의 코루틴 실패가 다른 코루틴에 영향을 주지 않습니다.
 */
@Configuration
class CoroutineConfig {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    @Bean
    fun applicationScope(): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            val coroutineName = context[CoroutineName]?.name ?: "unknown"
            logger.error(throwable) { "Uncaught exception in coroutine '$coroutineName'" }
        }

        return CoroutineScope(
            SupervisorJob() + Dispatchers.Default + exceptionHandler + CoroutineName("applicationScope"),
        )
    }
}
