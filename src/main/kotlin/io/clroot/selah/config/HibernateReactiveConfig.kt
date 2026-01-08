package io.clroot.selah.config

import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import io.clroot.selah.domains.prayer.adapter.outbound.persistence.LookbackSelectionEntity
import io.clroot.selah.domains.prayer.adapter.outbound.persistence.PrayerEntity
import io.clroot.selah.domains.prayer.adapter.outbound.persistence.PrayerPrayerTopicEntity
import io.clroot.selah.domains.prayer.adapter.outbound.persistence.PrayerTopicEntity
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.hibernate.reactive.mutiny.Mutiny
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration as SpringConfiguration

@SpringConfiguration
class HibernateReactiveConfig(
    @Value($$"${spring.datasource.url}") private val jdbcUrl: String,
    @Value($$"${spring.datasource.username}") private val username: String,
    @Value($$"${spring.datasource.password}") private val password: String,
) {
    @Bean
    fun prayerReactiveSessionFactory(): Mutiny.SessionFactory {
        val reactiveUrl = jdbcUrl.replace("jdbc:", "")

        val configuration =
            Configuration()
                .addAnnotatedClass(PrayerTopicEntity::class.java)
                .addAnnotatedClass(PrayerEntity::class.java)
                .addAnnotatedClass(PrayerPrayerTopicEntity::class.java)
                .addAnnotatedClass(LookbackSelectionEntity::class.java)
                .setProperty(AvailableSettings.JAKARTA_JDBC_URL, reactiveUrl)
                .setProperty(AvailableSettings.JAKARTA_JDBC_USER, username)
                .setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, password)
                .setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty(AvailableSettings.HBM2DDL_AUTO, "none")
                .setProperty(AvailableSettings.SHOW_SQL, "false")
                .setProperty(AvailableSettings.FORMAT_SQL, "false")
                .setProperty("hibernate.connection.pool_size", "10")

        val serviceRegistry =
            ReactiveServiceRegistryBuilder()
                .applySettings(configuration.properties)
                .build()

        return configuration
            .buildSessionFactory(serviceRegistry)
            .unwrap(Mutiny.SessionFactory::class.java)
    }

    @Bean
    fun jpqlRenderContext(): JpqlRenderContext = JpqlRenderContext()
}
