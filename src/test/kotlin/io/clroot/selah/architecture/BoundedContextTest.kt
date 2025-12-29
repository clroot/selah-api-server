package io.clroot.selah.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.clroot.selah.common.event.DomainEvent
import io.clroot.selah.common.event.IntegrationEvent
import io.kotest.core.spec.style.DescribeSpec

class BoundedContextTest :
    DescribeSpec({

        val basePackage = "io.clroot.selah"
        val importedClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(basePackage)

        describe("Bounded Context 독립성") {

            context("Member Context") {

                it("Member Domain은 다른 Context의 Domain에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.member.domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.prayer.domain..")
                        .because("Member Domain은 다른 Bounded Context의 Domain에 직접 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Prayer Context") {

                it("Prayer Domain은 다른 Context의 Domain에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.prayer.domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.member.domain..")
                        .because("Prayer Domain은 다른 Bounded Context의 Domain에 직접 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Cross-Context 통신") {

                it("Member Application은 다른 Context의 Adapter에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.member.application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.prayer.adapter..")
                        .because("Context 간 통신은 Port/Event를 통해야 하며, Adapter를 직접 참조하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Prayer Application은 다른 Context의 Adapter에 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.prayer.application..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.member.adapter..")
                        .because("Context 간 통신은 Port/Event를 통해야 하며, Adapter를 직접 참조하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }

        describe("이벤트 패키지 규칙") {

            context("Integration Event 위치") {

                it("IntegrationEvent 구현체는 application/event/ 패키지에만 위치해야 한다") {
                    classes()
                        .that()
                        .areAssignableTo(IntegrationEvent::class.java)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveSimpleName("BaseIntegrationEvent")
                        .should()
                        .resideInAPackage("..application.event..")
                        .because("IntegrationEvent는 Application Layer에서 발행되며 application/event/ 패키지에 위치해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Member IntegrationEvent는 다른 Context의 Domain 객체를 참조하지 않는다") {
                    noClasses()
                        .that()
                        .areAssignableTo(IntegrationEvent::class.java)
                        .and()
                        .resideInAPackage("..domains.member..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.prayer.domain..")
                        .because("IntegrationEvent는 Context 경계를 넘어 전달되므로 다른 Context의 Domain을 참조하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Prayer IntegrationEvent는 다른 Context의 Domain 객체를 참조하지 않는다") {
                    noClasses()
                        .that()
                        .areAssignableTo(IntegrationEvent::class.java)
                        .and()
                        .resideInAPackage("..domains.prayer..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.member.domain..")
                        .because("IntegrationEvent는 Context 경계를 넘어 전달되므로 다른 Context의 Domain을 참조하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Domain Event 위치") {

                it("DomainEvent 구현체는 domain/event/ 패키지에만 위치해야 한다") {
                    classes()
                        .that()
                        .areAssignableTo(DomainEvent::class.java)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveSimpleName("BaseDomainEvent")
                        .should()
                        .resideInAPackage("..domain.event..")
                        .because("DomainEvent는 Domain Layer에서 발생하며 domain/event/ 패키지에 위치해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("Integration Event Handler 위치") {

                it("Member Context에서 adapter/in/event/ 외부는 다른 Context의 IntegrationEvent를 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.member..")
                        .and()
                        .resideOutsideOfPackage("..adapter.inbound.event..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.prayer.application.event..")
                        .because("다른 Context의 IntegrationEvent를 수신하려면 adapter/in/event/에 위치해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }

                it("Prayer Context에서 adapter/in/event/ 외부는 다른 Context의 IntegrationEvent를 의존하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domains.prayer..")
                        .and()
                        .resideOutsideOfPackage("..adapter.inbound.event..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domains.member.application.event..")
                        .because("다른 Context의 IntegrationEvent를 수신하려면 adapter/in/event/에 위치해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }
    })
