package io.clroot.selah.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import io.kotest.core.spec.style.DescribeSpec

class HexagonalArchitectureTest :
    DescribeSpec({

        val basePackage = "io.clroot.selah"
        val importedClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(basePackage)

        describe("Hexagonal Architecture") {

            context("레이어 의존성 규칙") {

                it("Domain은 Application, Adapter에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..application..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("..adapter..")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Application은 Adapter에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..adapter..")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Layered Architecture 검증 (Adapter -> Application -> Domain)") {
                    layeredArchitecture()
                        .consideringOnlyDependenciesInLayers()
                        .layer("Domain")
                        .definedBy("..domains..domain..")
                        .layer("Application")
                        .definedBy("..domains..application..")
                        .layer("Adapter")
                        .definedBy("..domains..adapter..")
                        .layer("Common")
                        .definedBy("..common..")
                        .whereLayer("Domain")
                        .mayOnlyAccessLayers("Common")
                        .whereLayer("Application")
                        .mayOnlyAccessLayers("Domain", "Common")
                        .whereLayer("Adapter")
                        .mayOnlyAccessLayers("Application", "Domain", "Common")
                        .whereLayer("Common")
                        .mayNotAccessAnyLayer()
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Port 의존성 규칙") {

                it("Input Port는 Domain에만 의존한다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..port.inbound..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..adapter..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("..service..")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Output Port는 Domain에만 의존한다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..port.outbound..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..adapter..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("..service..")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }
    })
