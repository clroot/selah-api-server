package io.clroot.selah.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.DescribeSpec

class DomainLayerTest :
    DescribeSpec({

        val basePackage = "io.clroot.selah"
        val importedClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(basePackage)

        describe("Domain Layer 순수성") {

            context("외부 프레임워크 의존 금지") {

                it("Domain은 Spring Framework에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("org.springframework..")
                        .because("Domain Layer는 Spring에 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Domain은 JPA/Hibernate에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("jakarta.persistence..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("javax.persistence..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("org.hibernate..")
                        .because("Domain Layer는 JPA/Hibernate에 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Domain은 HTTP Client에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("okhttp3..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("retrofit2..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("org.apache.http..")
                        .because("Domain Layer는 HTTP Client에 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Domain은 Jackson에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("com.fasterxml.jackson..")
                        .because("Domain Layer는 JSON 직렬화 라이브러리에 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("JPA 어노테이션 금지") {

                it("Domain 클래스는 @Entity를 사용하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .beAnnotatedWith("jakarta.persistence.Entity")
                        .because("@Entity는 Adapter Layer의 Entity 클래스에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Domain 클래스는 @Table을 사용하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .beAnnotatedWith("jakarta.persistence.Table")
                        .because("@Table은 Adapter Layer의 Entity 클래스에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Domain 클래스는 @Column을 사용하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .beAnnotatedWith("jakarta.persistence.Column")
                        .because("@Column은 Adapter Layer의 Entity 클래스에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("허용되는 의존성") {

                it("Domain은 순수 Kotlin/Java와 common 패키지만 사용할 수 있다") {
                    classes()
                        .that()
                        .resideInAPackage("..domains..domain..")
                        .should()
                        .onlyDependOnClassesThat()
                        .resideInAnyPackage(
                            "kotlin..",
                            "java..",
                            "org.jetbrains.annotations..", // Kotlin 컴파일러 어노테이션
                            "..domains..domain..",
                            "..common.event..",
                            "..common.domain..", // AggregateRoot, AggregateId
                            "..common.vo..", // Shared Kernel Value Objects
                            "..common.util..", // ULIDSupport (ID 생성용)
                        ).because("Domain Layer는 순수 Kotlin/Java와 공유 모듈(event, domain, vo, util)만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }
    })
