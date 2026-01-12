package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.security.MessageDigest
import java.time.LocalDateTime

@SpringBootTest
class EmailVerificationTokenPersistenceAdapterIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: EmailVerificationTokenPersistenceAdapter

    @Autowired
    private lateinit var sessionFactory: Mutiny.SessionFactory

    @Autowired
    private lateinit var jpqlRenderContext: JpqlRenderContext

    init {
        describe("EmailVerificationTokenPersistenceAdapter") {
            afterEach {
                sessionFactory
                    .withTransaction { session ->
                        session
                            .createMutationQuery(
                                jpql { deleteFrom(entity(EmailVerificationTokenEntity::class)) },
                                jpqlRenderContext,
                            ).executeUpdate()
                    }.awaitSuspending()
            }

            describe("create") {
                context("새 토큰을 생성할 때") {
                    it("토큰이 정상적으로 생성되고 저장된다") {
                        val memberId = MemberId.new()

                        val result = adapter.create(memberId)

                        result.shouldNotBeNull()
                        result.rawToken shouldHaveLength 64
                        result.info.memberId shouldBe memberId
                        result.info.usedAt.shouldBeNull()
                        result.info.isValid() shouldBe true

                        val entity =
                            sessionFactory
                                .withSession { session ->
                                    session.find(EmailVerificationTokenEntity::class.java, result.info.id)
                                }.awaitSuspending()
                        entity.shouldNotBeNull()
                        entity.memberId shouldBe memberId.value
                        entity.tokenHash shouldHaveLength 64
                    }
                }
            }

            describe("findValidByToken") {
                context("유효한 토큰으로 조회할 때") {
                    it("토큰 정보를 반환한다") {
                        val memberId = MemberId.new()
                        val createResult = adapter.create(memberId)

                        val found = adapter.findValidByToken(createResult.rawToken)

                        found.shouldNotBeNull()
                        found.id shouldBe createResult.info.id
                        found.memberId shouldBe memberId
                        found.isValid() shouldBe true
                    }
                }

                context("존재하지 않는 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        val found = adapter.findValidByToken("nonexistent-token")

                        found.shouldBeNull()
                    }
                }

                context("만료된 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        val memberId = MemberId.new()
                        val now = LocalDateTime.now()

                        val rawToken = "test-expired-token-12345"
                        val tokenHash = hashToken(rawToken)

                        val expiredEntity =
                            EmailVerificationTokenEntity(
                                id = "expired-token-id",
                                tokenHash = tokenHash,
                                memberId = memberId.value,
                                expiresAt = now.minusHours(1),
                                createdAt = now.minusDays(2),
                            )
                        sessionFactory
                            .withTransaction { session ->
                                session.persist(expiredEntity)
                            }.awaitSuspending()

                        val found = adapter.findValidByToken(rawToken)

                        found.shouldBeNull()
                    }
                }

                context("이미 사용된 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        val memberId = MemberId.new()
                        val createResult = adapter.create(memberId)

                        adapter.markAsUsed(createResult.info.id)

                        val found = adapter.findValidByToken(createResult.rawToken)

                        found.shouldBeNull()
                    }
                }
            }

            describe("markAsUsed") {
                context("토큰을 사용 처리할 때") {
                    it("usedAt이 설정된다") {
                        val memberId = MemberId.new()
                        val createResult = adapter.create(memberId)

                        adapter.markAsUsed(createResult.info.id)

                        val entity =
                            sessionFactory
                                .withSession { session ->
                                    session.find(EmailVerificationTokenEntity::class.java, createResult.info.id)
                                }.awaitSuspending()
                        entity.shouldNotBeNull()
                        entity.usedAt.shouldNotBeNull()
                    }
                }

                context("존재하지 않는 토큰 ID로 호출할 때") {
                    it("예외 없이 무시된다") {
                        adapter.markAsUsed("nonexistent-id")
                    }
                }
            }

            describe("invalidateAllByMemberId") {
                context("회원의 모든 토큰을 무효화할 때") {
                    it("해당 회원의 모든 토큰이 삭제된다") {
                        val memberId = MemberId.new()
                        val otherMemberId = MemberId.new()

                        adapter.create(memberId)
                        adapter.create(memberId)

                        val otherToken = adapter.create(otherMemberId)

                        adapter.invalidateAllByMemberId(memberId)

                        val memberTokenCount =
                            sessionFactory
                                .withSession { session ->
                                    session
                                        .createQuery(
                                            jpql {
                                                select(count(entity(EmailVerificationTokenEntity::class)))
                                                    .from(entity(EmailVerificationTokenEntity::class))
                                                    .where(path(EmailVerificationTokenEntity::memberId).eq(memberId.value))
                                            },
                                            jpqlRenderContext,
                                        ).singleResult
                                }.awaitSuspending()
                        memberTokenCount shouldBe 0L

                        val otherEntity =
                            sessionFactory
                                .withSession { session ->
                                    session.find(EmailVerificationTokenEntity::class.java, otherToken.info.id)
                                }.awaitSuspending()
                        otherEntity.shouldNotBeNull()
                    }
                }
            }

            describe("findLatestCreatedAtByMemberId") {
                context("토큰이 존재하는 회원일 때") {
                    it("가장 최근 생성 시간을 반환한다") {
                        val memberId = MemberId.new()

                        adapter.create(memberId)
                        Thread.sleep(100)
                        val latestToken = adapter.create(memberId)

                        val latestCreatedAt = adapter.findLatestCreatedAtByMemberId(memberId)

                        latestCreatedAt.shouldNotBeNull()
                        latestCreatedAt shouldBe latestToken.info.createdAt
                    }
                }

                context("토큰이 없는 회원일 때") {
                    it("null을 반환한다") {
                        val memberId = MemberId.new()

                        val latestCreatedAt = adapter.findLatestCreatedAtByMemberId(memberId)

                        latestCreatedAt.shouldBeNull()
                    }
                }
            }

            describe("deleteExpiredTokens") {
                context("만료된 토큰이 존재할 때") {
                    it("만료된 토큰만 삭제되고 삭제된 개수를 반환한다") {
                        val memberId = MemberId.new()
                        val now = LocalDateTime.now()

                        val expiredEntity =
                            EmailVerificationTokenEntity(
                                id = "expired-token-1",
                                tokenHash = "expired-hash-1",
                                memberId = memberId.value,
                                expiresAt = now.minusHours(1),
                                createdAt = now.minusDays(2),
                            )
                        sessionFactory
                            .withTransaction { session ->
                                session.persist(expiredEntity)
                            }.awaitSuspending()

                        val validToken = adapter.create(MemberId.new())

                        val deletedCount = adapter.deleteExpiredTokens()

                        deletedCount shouldBe 1

                        val expiredEntityAfter =
                            sessionFactory
                                .withSession { session ->
                                    session.find(EmailVerificationTokenEntity::class.java, "expired-token-1")
                                }.awaitSuspending()
                        expiredEntityAfter.shouldBeNull()

                        val validEntityAfter =
                            sessionFactory
                                .withSession { session ->
                                    session.find(EmailVerificationTokenEntity::class.java, validToken.info.id)
                                }.awaitSuspending()
                        validEntityAfter.shouldNotBeNull()
                    }
                }

                context("만료된 토큰이 없을 때") {
                    it("0을 반환한다") {
                        adapter.create(MemberId.new())

                        val deletedCount = adapter.deleteExpiredTokens()

                        deletedCount shouldBe 0
                    }
                }
            }
        }
    }
}

private fun hashToken(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}
