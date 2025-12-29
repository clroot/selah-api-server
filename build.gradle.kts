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

val kotlinJdslVersion = "3.6.1"
val kotestVersion = "6.0.7"
val coroutinesVersion = "1.10.2"
val testcontainersVersion = "1.20.4"

dependencies {
    // ===== Kotlin =====
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ===== Kotlin Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // ===== Spring Boot =====
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ===== Spring AOP =====
    implementation("org.springframework:spring-aspects")

    // ===== Kotlin JDSL =====
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:$kotlinJdslVersion")

    // ===== Database =====
    runtimeOnly("org.postgresql:postgresql")

    // ===== Database Migration =====
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    // ===== Crypto =====
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")

    // ===== Logging =====
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // ===== Testing: Kotest =====
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    // ===== Testing: MockK =====
    testImplementation("io.mockk:mockk:1.13.10")

    // ===== Testing: Spring =====
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // ===== Testing: Architecture =====
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // ===== TestFixtures =====
    testFixturesImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesApi("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesApi("io.kotest:kotest-assertions-core:$kotestVersion")
    testFixturesApi("io.kotest:kotest-extensions-spring:$kotestVersion")
    testFixturesApi("org.testcontainers:testcontainers:$testcontainersVersion")
    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testFixturesApi("org.postgresql:postgresql")
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
