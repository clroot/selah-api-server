package io.clroot.selah.test.container

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.spec.Spec

/**
 * Kotest Spec 레벨 Extension
 *
 * 각 테스트 Spec이 시작될 때 컨테이너를 시작하고 시스템 프로퍼티를 설정합니다.
 */
class DatabaseTestExtension : SpecExtension {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun intercept(
        spec: Spec,
        execute: suspend (Spec) -> Unit,
    ) {
        // 컨테이너 시작 (이미 시작된 경우 재사용)
        PostgreSQLTestContainer.start()
        PostgreSQLTestContainer.configureSystemProperties()

        logger.debug { "${spec::class.simpleName} 테스트 시작 - PostgreSQL: ${PostgreSQLTestContainer.instance.jdbcUrl}" }

        try {
            execute(spec)
        } finally {
            logger.debug { "${spec::class.simpleName} 테스트 완료" }
        }
    }
}
