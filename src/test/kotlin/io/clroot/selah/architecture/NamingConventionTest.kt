package io.clroot.selah.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import io.kotest.core.spec.style.DescribeSpec

class NamingConventionTest :
    DescribeSpec({

        val basePackage = "io.clroot.selah"
        val importedClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(basePackage)

        describe("Naming Convention") {

            context("UseCase 네이밍") {

                it("port.inbound 패키지의 인터페이스는 UseCase로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..port.inbound..")
                        .and()
                        .areInterfaces()
                        .should()
                        .haveSimpleNameEndingWith("UseCase")
                        .because("Input Port 인터페이스는 ~UseCase로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Port 네이밍") {

                it("port.out 패키지의 인터페이스는 Port로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..port.outbound..")
                        .and()
                        .areInterfaces()
                        .should()
                        .haveSimpleNameEndingWith("Port")
                        .because("Output Port 인터페이스는 ~Port로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Service 네이밍") {

                it("service 패키지의 클래스는 Service로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..application.service..")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areTopLevelClasses() // Kotlin 컴파일러 생성 내부 클래스 제외 (suspend function continuations)
                        .should()
                        .haveSimpleNameEndingWith("Service")
                        .because("Service 클래스는 ~Service로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Adapter 네이밍") {

                it("adapter.out.persistence 패키지의 클래스는 Adapter 또는 Repository로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..adapter.outbound.persistence..")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areTopLevelClasses() // Kotlin 컴파일러 생성 내부 클래스 제외
                        .and()
                        .haveSimpleNameNotEndingWith("Entity")
                        .and()
                        .haveSimpleNameNotEndingWith("Mapper")
                        .and()
                        .haveSimpleNameNotEndingWith("Id") // JPA composite key 클래스 제외
                        .should()
                        .haveSimpleNameEndingWith("Adapter")
                        .orShould()
                        .haveSimpleNameEndingWith("Repository")
                        .because("Persistence Adapter는 ~Adapter 또는 ~Repository로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("JPA Entity 클래스는 Entity로 끝난다") {
                    classes()
                        .that()
                        .areAnnotatedWith("jakarta.persistence.Entity")
                        .should()
                        .haveSimpleNameEndingWith("Entity")
                        .because("JPA Entity 클래스는 ~Entity로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Mapper 클래스는 Mapper로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..adapter..")
                        .and()
                        .haveSimpleNameContaining("Mapper")
                        .should()
                        .haveSimpleNameEndingWith("Mapper")
                        .because("Mapper 클래스는 ~Mapper로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Controller 네이밍") {

                it("adapter.inbound.web 패키지의 클래스는 Controller로 끝난다") {
                    classes()
                        .that()
                        .resideInAPackage("..adapter.inbound.web..")
                        .and()
                        .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                        .should()
                        .haveSimpleNameEndingWith("Controller")
                        .because("REST Controller는 ~Controller로 명명해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }
    })
