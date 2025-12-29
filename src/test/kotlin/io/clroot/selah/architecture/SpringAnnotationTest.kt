package io.clroot.selah.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.DescribeSpec

class SpringAnnotationTest :
    DescribeSpec({

        val basePackage = "io.clroot.selah"
        val importedClasses: JavaClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(basePackage)

        describe("Spring Annotation 사용 위치") {

            context("@Service 어노테이션") {

                it("@Service는 application.service 패키지에서만 사용한다") {
                    classes()
                        .that()
                        .areAnnotatedWith("org.springframework.stereotype.Service")
                        .should()
                        .resideInAPackage("..application.service..")
                        .because("@Service는 Application Layer의 Service에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("@Component 어노테이션") {

                it("@Component는 Adapter Layer에서만 사용한다") {
                    classes()
                        .that()
                        .areAnnotatedWith("org.springframework.stereotype.Component")
                        .should()
                        .resideInAPackage("..adapter..")
                        .orShould()
                        .resideInAPackage("..common..")
                        .because("@Component는 Adapter Layer 또는 Common에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("@Repository 어노테이션") {

                it("@Repository는 adapter.out 패키지에서만 사용한다") {
                    classes()
                        .that()
                        .areAnnotatedWith("org.springframework.stereotype.Repository")
                        .should()
                        .resideInAPackage("..adapter.outbound..")
                        .because("@Repository는 Output Adapter에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("@RestController 어노테이션") {

                it("@RestController는 adapter.inbound.web 패키지에서만 사용한다") {
                    classes()
                        .that()
                        .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                        .should()
                        .resideInAPackage("..adapter.inbound.web..")
                        .because("@RestController는 Web Input Adapter에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("@Transactional 어노테이션") {

                it("@Transactional은 Domain에서 사용하지 않는다") {
                    noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                        .because("Domain Layer는 Spring @Transactional에 의존하면 안됩니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }

            context("JPA 어노테이션") {

                it("@Entity는 adapter.out.persistence 패키지에서만 사용한다") {
                    classes()
                        .that()
                        .areAnnotatedWith("jakarta.persistence.Entity")
                        .should()
                        .resideInAPackage("..adapter.outbound.persistence..")
                        .orShould()
                        .resideInAPackage("..common.adapter.persistence..")
                        .because("@Entity는 Persistence Adapter에서만 사용해야 합니다")
                        .allowEmptyShould(true)
                        .check(importedClasses)
                }
            }
        }
    })
