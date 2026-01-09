package io.clroot.selah.common.reactive

import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import jakarta.persistence.Entity
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.hibernate.reactive.mutiny.Mutiny
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.context.annotation.Configuration as SpringConfiguration

@SpringConfiguration
class HibernateReactiveConfig(
    private val applicationContext: ApplicationContext,
    @Value($$"${spring.datasource.url}") private val jdbcUrl: String,
    @Value($$"${spring.datasource.username}") private val username: String,
    @Value($$"${spring.datasource.password}") private val password: String,
    @Value($$"${spring.jpa.database-platform}") private val dialect: String,
    @Value($$"${spring.jpa.hibernate.ddl-auto}") private val ddlAuto: String,
    @Value($$"${spring.jpa.show-sql}") private val showSql: String,
    @Value("\${spring.jpa.properties.hibernate.format_sql}") private val formatSql: String,
    @Value($$"${hibernate.reactive.connection.pool_size:10}") private val poolSize: String,
) {
    @Bean
    fun prayerReactiveSessionFactory(): Mutiny.SessionFactory {
        val reactiveUrl = jdbcUrl.replace("jdbc:", "")

        val configuration =
            Configuration().apply {
                // @SpringBootApplication 패키지 기준으로 @Entity 클래스 자동 스캔
                val basePackages = AutoConfigurationPackages.get(applicationContext)
                findEntityClasses(basePackages).forEach { entityClass ->
                    addAnnotatedClass(entityClass)
                }
            }
                .setProperty(AvailableSettings.JAKARTA_JDBC_URL, reactiveUrl)
                .setProperty(AvailableSettings.JAKARTA_JDBC_USER, username)
                .setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, password)
                .setProperty(AvailableSettings.DIALECT, dialect)
                .setProperty(AvailableSettings.HBM2DDL_AUTO, ddlAuto)
                .setProperty(AvailableSettings.SHOW_SQL, showSql)
                .setProperty(AvailableSettings.FORMAT_SQL, formatSql)
                .setProperty("hibernate.connection.pool_size", poolSize)

        val serviceRegistry =
            ReactiveServiceRegistryBuilder()
                .applySettings(configuration.properties)
                .build()

        return configuration
            .buildSessionFactory(serviceRegistry)
            .unwrap(Mutiny.SessionFactory::class.java)
    }

    private fun findEntityClasses(basePackages: List<String>): List<Class<*>> {
        val scanner =
            ClassPathScanningCandidateComponentProvider(false).apply {
                addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
            }

        return basePackages.flatMap { basePackage ->
            scanner
                .findCandidateComponents(basePackage)
                .mapNotNull { it.beanClassName }
                .map { Class.forName(it) }
        }
    }

    @Bean
    fun jpqlRenderContext(): JpqlRenderContext = JpqlRenderContext()
}
