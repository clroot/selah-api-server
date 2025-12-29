import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.allopen") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"

    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"

    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"

    `java-test-fixtures`
}

group = "io.clroot.selah"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val jjwtVersion = "0.12.6"
val kotlinJdslVersion = "3.6.1"
val kotestVersion = "6.0.7"
val coroutinesVersion = "1.10.2"
val testcontainersVersion = "1.20.4"

dependencies {
    // ===== Kotlin =====
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // ===== Kotlin Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")

    // ===== Spring Boot Core =====
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // ===== Spring Web =====
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("jakarta.validation:jakarta.validation-api")

    // ===== Spring Security =====
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ===== Spring Data / Persistence =====
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-aspects")

    // ===== Spring Mail =====
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // ===== Hibernate / JPA =====
    implementation("org.hibernate.orm:hibernate-core")

    // ===== Kotlin JDSL =====
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:$kotlinJdslVersion")

    // ===== Database =====
    implementation("com.zaxxer:HikariCP")
    runtimeOnly("org.postgresql:postgresql")

    // ===== JWT =====
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // ===== Crypto =====
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")

    // ===== Logging =====
    implementation("org.slf4j:slf4j-api")

    // ===== Testing: Kotest =====
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    // ===== Testing: MockK =====
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // ===== Testing: Spring =====
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    // ===== Testing: Architecture =====
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // ===== TestFixtures =====
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesApi("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesApi("io.kotest:kotest-assertions-core:$kotestVersion")
    testFixturesApi("io.kotest:kotest-extensions-spring:$kotestVersion")
    testFixturesApi("org.testcontainers:testcontainers:$testcontainersVersion")
    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testFixturesApi("org.postgresql:postgresql")
    testFixturesApi("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xcontext-receivers",
                "-opt-in=kotlin.ExperimentalValueClassApi",
                "-Xannotation-default-target=param-property",
            )
        }
    }
}
