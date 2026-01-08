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
    @Value($$"${spring.jpa.database-platform}") private val dialect: String,
    @Value($$"${spring.jpa.hibernate.ddl-auto}") private val ddlAuto: String,
    @Value($$"${spring.jpa.show-sql}") private val showSql: String,
    @Value($$"${spring.jpa.properties.hibernate.format_sql}") private val formatSql: String,
    @Value($$"${selah.reactive.connection.pool_size:10}") private val poolSize: String,
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

    @Bean
    fun jpqlRenderContext(): JpqlRenderContext = JpqlRenderContext()
}
