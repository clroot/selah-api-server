package io.clroot.selah.test.container

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 싱글톤 PostgreSQL TestContainer
 *
 * 모든 테스트에서 하나의 컨테이너를 공유하여 테스트 속도를 향상시킵니다.
 */
object PostgreSQLTestContainer {
    private const val POSTGRES_IMAGE = "postgres:16-alpine"
    private const val DATABASE_NAME = "selah_test"
    private const val USERNAME = "test"
    private const val PASSWORD = "test"

    val instance: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE))
            .apply {
                withDatabaseName(DATABASE_NAME)
                withUsername(USERNAME)
                withPassword(PASSWORD)
                withReuse(true) // 컨테이너 재사용 설정
            }
    }

    /**
     * 컨테이너를 시작하고 JDBC URL을 반환합니다.
     */
    fun start(): String {
        if (!instance.isRunning) {
            instance.start()
        }
        return instance.jdbcUrl
    }

    /**
     * 컨테이너를 중지합니다.
     */
    fun stop() {
        if (instance.isRunning) {
            instance.stop()
        }
    }

    /**
     * Spring Boot 테스트를 위한 시스템 프로퍼티 설정
     */
    fun configureSystemProperties() {
        System.setProperty("spring.datasource.url", instance.jdbcUrl)
        System.setProperty("spring.datasource.username", instance.username)
        System.setProperty("spring.datasource.password", instance.password)
        System.setProperty("spring.datasource.driver-class-name", instance.driverClassName)
    }
}
