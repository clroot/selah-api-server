package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class SessionPersistenceAdapterIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var sessionPersistenceAdapter: SessionPersistenceAdapter

    @Autowired
    private lateinit var sessionFactory: Mutiny.SessionFactory

    @Autowired
    private lateinit var jpqlRenderContext: JpqlRenderContext

    init {
        describe("SessionPersistenceAdapter") {
            afterEach {
                sessionFactory
                    .withTransaction { session ->
                        session
                            .createMutationQuery(
                                jpql { deleteFrom(entity(SessionEntity::class)) },
                                jpqlRenderContext,
                            ).executeUpdate()
                    }.awaitSuspending()
            }

            describe("create") {
                context("새 세션을 생성할 때") {
                    it("세션이 정상적으로 생성되고 저장된다") {
                        val memberId = MemberId.new()
                        val role = Member.Role.USER
                        val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
                        val ipAddress = "192.168.1.100"

                        val sessionInfo =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = role,
                                userAgent = userAgent,
                                ipAddress = ipAddress,
                            )

                        sessionInfo.shouldNotBeNull()
                        sessionInfo.token.shouldNotBeNull()
                        sessionInfo.memberId shouldBe memberId
                        sessionInfo.role shouldBe role
                        sessionInfo.userAgent shouldBe userAgent
                        sessionInfo.createdIp shouldBe ipAddress
                        sessionInfo.lastAccessedIp shouldBe ipAddress
                        sessionInfo.expiresAt shouldNotBe null
                        sessionInfo.createdAt shouldNotBe null

                        val entity =
                            sessionFactory
                                .withSession { session ->
                                    session.find(SessionEntity::class.java, sessionInfo.token)
                                }.awaitSuspending()
                        entity.shouldNotBeNull()
                        entity.memberId shouldBe memberId.value
                    }
                }

                context("userAgent와 ipAddress가 null일 때") {
                    it("null 값으로 세션이 생성된다") {
                        val memberId = MemberId.new()

                        val sessionInfo =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = null,
                                ipAddress = null,
                            )

                        sessionInfo.userAgent.shouldBeNull()
                        sessionInfo.createdIp.shouldBeNull()
                        sessionInfo.lastAccessedIp.shouldBeNull()
                    }
                }

                context("긴 userAgent가 주어졌을 때") {
                    it("500자로 잘려서 저장된다") {
                        val memberId = MemberId.new()
                        val longUserAgent = "a".repeat(1000)

                        val sessionInfo =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = longUserAgent,
                                ipAddress = null,
                            )

                        sessionInfo.userAgent?.length shouldBe 500
                    }
                }
            }

            describe("findByToken") {
                context("존재하는 토큰으로 조회할 때") {
                    it("세션 정보를 반환한다") {
                        val memberId = MemberId.new()
                        val createdSession =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = "Test Agent",
                                ipAddress = "10.0.0.1",
                            )

                        val foundSession = sessionPersistenceAdapter.findByToken(createdSession.token)

                        foundSession.shouldNotBeNull()
                        foundSession.token shouldBe createdSession.token
                        foundSession.memberId shouldBe memberId
                    }
                }

                context("존재하지 않는 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        val foundSession = sessionPersistenceAdapter.findByToken("nonexistent-token")

                        foundSession.shouldBeNull()
                    }
                }
            }

            describe("delete") {
                context("존재하는 세션을 삭제할 때") {
                    it("세션이 삭제된다") {
                        val memberId = MemberId.new()
                        val session =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = null,
                                ipAddress = null,
                            )

                        sessionPersistenceAdapter.delete(session.token)

                        val foundSession = sessionPersistenceAdapter.findByToken(session.token)
                        foundSession.shouldBeNull()
                    }
                }
            }

            describe("deleteAllByMemberId") {
                context("회원의 여러 세션이 존재할 때") {
                    it("해당 회원의 모든 세션이 삭제된다") {
                        val memberId = MemberId.new()
                        val otherMemberId = MemberId.new()

                        val session1 =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = "Session 1",
                                ipAddress = "1.1.1.1",
                            )
                        val session2 =
                            sessionPersistenceAdapter.create(
                                memberId = memberId,
                                role = Member.Role.USER,
                                userAgent = "Session 2",
                                ipAddress = "2.2.2.2",
                            )

                        val otherSession =
                            sessionPersistenceAdapter.create(
                                memberId = otherMemberId,
                                role = Member.Role.USER,
                                userAgent = "Other Session",
                                ipAddress = "3.3.3.3",
                            )

                        sessionPersistenceAdapter.deleteAllByMemberId(memberId)

                        sessionPersistenceAdapter.findByToken(session1.token).shouldBeNull()
                        sessionPersistenceAdapter.findByToken(session2.token).shouldBeNull()

                        sessionPersistenceAdapter.findByToken(otherSession.token).shouldNotBeNull()
                    }
                }
            }

            describe("extendExpiry") {
                context("세션 만료 시간이 threshold 이하일 때") {
                    it("만료 시간이 연장되고 마지막 접근 IP가 업데이트된다") {
                        val memberId = MemberId.new()
                        val token = "test-session-token-for-extend"
                        val now = LocalDateTime.now()
                        val shortExpiry = now.plusMinutes(5)

                        val shortExpirySession =
                            SessionEntity(
                                token = token,
                                memberId = memberId.value,
                                role = Member.Role.USER,
                                userAgent = null,
                                createdIp = "10.0.0.1",
                                lastAccessedIp = "10.0.0.1",
                                expiresAt = shortExpiry,
                                createdAt = now,
                            )
                        sessionFactory
                            .withTransaction { session ->
                                session.persist(shortExpirySession)
                            }.awaitSuspending()

                        val beforeExtend = sessionPersistenceAdapter.findByToken(token)
                        beforeExtend.shouldNotBeNull()
                        val remainingBeforeExtend =
                            java.time.Duration.between(
                                LocalDateTime.now(),
                                beforeExtend.expiresAt,
                            )
                        (remainingBeforeExtend.toMinutes() < 10) shouldBe true

                        val newIpAddress = "10.0.0.2"

                        sessionPersistenceAdapter.extendExpiry(token, newIpAddress)

                        val updatedSession = sessionPersistenceAdapter.findByToken(token)
                        updatedSession.shouldNotBeNull()
                        updatedSession.lastAccessedIp shouldBe newIpAddress

                        val expectedMinExpiry = LocalDateTime.now().plusMinutes(50)
                        (updatedSession.expiresAt > expectedMinExpiry) shouldBe true
                    }
                }

                context("존재하지 않는 토큰일 때") {
                    it("예외 없이 무시된다") {
                        sessionPersistenceAdapter.extendExpiry("nonexistent-token", "1.1.1.1")
                    }
                }
            }

            describe("deleteExpiredSessions") {
                context("만료된 세션이 존재할 때") {
                    it("만료된 세션만 삭제되고 삭제된 개수를 반환한다") {
                        val memberId = MemberId.new()

                        val expiredSession =
                            SessionEntity(
                                token = "expired-token-1",
                                memberId = memberId.value,
                                role = Member.Role.USER,
                                userAgent = null,
                                createdIp = null,
                                lastAccessedIp = null,
                                expiresAt = LocalDateTime.now().minusHours(1),
                                createdAt = LocalDateTime.now().minusDays(1),
                            )
                        sessionFactory
                            .withTransaction { session ->
                                session.persist(expiredSession)
                            }.awaitSuspending()

                        val validSession =
                            sessionPersistenceAdapter.create(
                                memberId = MemberId.new(),
                                role = Member.Role.USER,
                                userAgent = null,
                                ipAddress = null,
                            )

                        val deletedCount = sessionPersistenceAdapter.deleteExpiredSessions()

                        deletedCount shouldBe 1

                        sessionPersistenceAdapter.findByToken("expired-token-1").shouldBeNull()

                        sessionPersistenceAdapter.findByToken(validSession.token).shouldNotBeNull()
                    }
                }
            }
        }
    }
}
