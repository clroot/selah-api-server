import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.allopen") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"

    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"

    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"

    jacoco
    `java-test-fixtures`
}

group = "io.clroot.selah"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
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
val testcontainersCoreVersion = "2.0.2"
val mockkVersion = "1.14.7"
val hibernateReactiveVersion = "3.1.0.Final"
val hibernateReactiveCoroutinesVersion = "1.3.4"
val mutinyVersion = "2.6.0"
val vertxVersion = "4.5.23"

dependencies {
    // ===== Kotlin =====
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ===== Kotlin Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")

    // ===== Spring Boot =====
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.data:spring-data-commons") // Page, Pageable
    // developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ===== Spring AOP =====
    implementation("org.springframework:spring-aspects")

    // ===== Kotlin JDSL =====
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:hibernate-reactive-support:$kotlinJdslVersion") // Member + Prayer Context (Reactive)

    // ===== Hibernate Reactive =====
    implementation(
        "com.github.clroot.hibernate-reactive-coroutines:hibernate-reactive-coroutines-spring-boot-starter-boot4:$hibernateReactiveCoroutinesVersion",
    )

    // ===== Database =====
    runtimeOnly("org.postgresql:postgresql") // JDBC Driver (Liquibase + Test)
    implementation("io.vertx:vertx-core:$vertxVersion") // Vert.x Core (Reactive)
    implementation("io.vertx:vertx-pg-client:$vertxVersion") // Reactive Driver (Prayer Context Reactive)
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion") // Vert.x Coroutines Support

    // ===== Database Migration =====
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    // ===== Crypto =====
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")

    // ===== Email =====
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // ===== Logging =====
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // ===== Testing: Kotest =====
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

    // ===== Testing: MockK =====
    testImplementation("io.mockk:mockk:$mockkVersion")

    // ===== Testing: Spring =====
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // ===== Testing: Architecture =====
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // ===== TestFixtures =====
    testFixturesImplementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesApi("io.kotest:kotest-assertions-core:$kotestVersion")
    testFixturesApi("io.kotest:kotest-extensions-spring:$kotestVersion")
    testFixturesApi("org.testcontainers:testcontainers:$testcontainersCoreVersion")
    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testFixturesApi("org.postgresql:postgresql")
    testFixturesApi(
        "com.github.clroot.hibernate-reactive-coroutines:hibernate-reactive-coroutines-spring-boot-starter-boot4:$hibernateReactiveCoroutinesVersion",
    )
}

tasks {
    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            sourceSets.main.get().output.asFileTree.matching {
                exclude(
                    "**/entities/**",
                    "**/*Entity*",
                    "**/*MapperImpl*",
                    "**/*Application*",
                    "**/*Id*",
                    "**/*Request*",
                    "**/*Response*",
                    "**/*Command*",
                    "**/*Query*",
                    "**/*Dto*",
                    "**/*DTO*",
                    "**/*Properties*",
                )
            },
        )
    }

    jacocoTestCoverageVerification {
        violationRules {
            rule {
                element = "CLASS"
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.60".toBigDecimal()
                }
                excludes =
                    listOf(
                        "*.**Entity",
                        "*.**MapperImpl",
                        "*.**Application",
                        "*.**Id",
                        "*.**Request",
                        "*.**Response",
                        "*.**Command",
                        "*.**Query",
                        "*.**Dto",
                        "*.**DTO",
                        "*.**Properties",
                    )
            }

            // Persistence Adapter는 실제 DB 테스트가 필수이므로 높은 커버리지 강제
            rule {
                element = "CLASS"
                includes = listOf("*.*PersistenceAdapter")
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
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
