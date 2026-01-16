package io.clroot.selah.common.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Liquibase 마이그레이션이 Hibernate SessionFactory보다 먼저 실행되도록 보장하는 설정.
 *
 * Hibernate Reactive는 Vert.x PG Client를 사용하고, Liquibase는 JDBC DataSource를 사용하므로
 * 서로 독립적으로 초기화됩니다. 이로 인해 Hibernate가 스키마 검증(`ddl-auto: validate`)을 할 때
 * Liquibase 마이그레이션이 아직 완료되지 않아 테이블을 찾지 못하는 문제가 발생할 수 있습니다.
 *
 * 이 설정은 `hibernateSessionFactory` 빈이 `liquibase` 빈에 의존하도록 하여
 * 마이그레이션이 완료된 후에 Hibernate가 초기화되도록 합니다.
 */
@Configuration
class LiquibaseConfig {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    @Bean
    fun hibernateLiquibaseDependencyPostProcessor(): BeanFactoryPostProcessor =
        BeanFactoryPostProcessor { beanFactory ->
            val hibernateBeanName = "hibernateSessionFactory"
            val liquibaseBeanName = "liquibase"

            if (beanFactory.containsBeanDefinition(hibernateBeanName) &&
                beanFactory.containsBeanDefinition(liquibaseBeanName)
            ) {
                val beanDefinition = beanFactory.getBeanDefinition(hibernateBeanName)
                val existingDependencies = beanDefinition.dependsOn ?: emptyArray()

                if (!existingDependencies.contains(liquibaseBeanName)) {
                    beanDefinition.setDependsOn(*existingDependencies, liquibaseBeanName)
                    logger.info { "Configured '$hibernateBeanName' to depend on '$liquibaseBeanName' for proper initialization order" }
                }
            }
        }
}
