package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.security.MessageDigest
import java.time.LocalDateTime

@SpringBootTest
class EmailVerificationTokenPersistenceAdapterIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var adapter: EmailVerificationTokenPersistenceAdapter

    @Autowired
    private lateinit var repository: EmailVerificationTokenJpaRepository

    init {
        describe("EmailVerificationTokenPersistenceAdapter") {
            afterEach {
                repository.deleteAll()
            }

            describe("create") {
                context("새 토큰을 생성할 때") {
                    it("토큰이 정상적으로 생성되고 저장된다") {
                        val memberId = MemberId.new()

                        val result = adapter.create(memberId)

                        result.shouldNotBeNull()
                        result.rawToken shouldHaveLength 64 // 32 bytes * 2 (hex)
                        result.info.memberId shouldBe memberId
                        result.info.usedAt.shouldBeNull()
                        result.info.isValid() shouldBe true

                        // DB에서 직접 확인 (해시된 값으로 저장됨)
                        val entity = repository.findById(result.info.id).orElse(null)
                        entity.shouldNotBeNull()
                        entity.memberId shouldBe memberId.value
                        entity.tokenHash shouldHaveLength 64 // SHA-256 hash hex
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

                        // 이미 만료된 토큰을 직접 생성 (해시값을 알고 있어야 함)
                        val rawToken = "test-expired-token-12345"
                        val tokenHash = hashToken(rawToken)

                        val expiredEntity = EmailVerificationTokenEntity(
                            id = "expired-token-id",
                            tokenHash = tokenHash,
                            memberId = memberId.value,
                            expiresAt = now.minusHours(1), // 1시간 전에 만료됨
                            createdAt = now.minusDays(2),
                        )
                        repository.save(expiredEntity)

                        val found = adapter.findValidByToken(rawToken)

                        found.shouldBeNull()
                    }
                }

                context("이미 사용된 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        val memberId = MemberId.new()
                        val createResult = adapter.create(memberId)

                        // 토큰을 사용 처리
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

                        val entity = repository.findById(createResult.info.id).orElse(null)
                        entity.shouldNotBeNull()
                        entity.usedAt.shouldNotBeNull()
                    }
                }

                context("존재하지 않는 토큰 ID로 호출할 때") {
                    it("예외 없이 무시된다") {
                        // 예외가 발생하지 않아야 함
                        adapter.markAsUsed("nonexistent-id")
                    }
                }
            }

            describe("invalidateAllByMemberId") {
                context("회원의 모든 토큰을 무효화할 때") {
                    it("해당 회원의 모든 토큰이 삭제된다") {
                        val memberId = MemberId.new()
                        val otherMemberId = MemberId.new()

                        // 동일 회원의 여러 토큰 생성
                        adapter.create(memberId)
                        adapter.create(memberId)

                        // 다른 회원의 토큰 생성
                        val otherToken = adapter.create(otherMemberId)

                        // 회원의 모든 토큰 무효화
                        adapter.invalidateAllByMemberId(memberId)

                        // 해당 회원의 토큰은 모두 삭제됨
                        val memberTokens = repository.findAll().filter { it.memberId == memberId.value }
                        memberTokens.size shouldBe 0

                        // 다른 회원의 토큰은 유지됨
                        val otherEntity = repository.findById(otherToken.info.id).orElse(null)
                        otherEntity.shouldNotBeNull()
                    }
                }
            }

            describe("findLatestCreatedAtByMemberId") {
                context("토큰이 존재하는 회원일 때") {
                    it("가장 최근 생성 시간을 반환한다") {
                        val memberId = MemberId.new()

                        // 토큰 생성 (시간 간격 두고)
                        adapter.create(memberId)
                        Thread.sleep(100) // 시간 차이 확보
                        val latestToken = adapter.create(memberId)

                        val latestCreatedAt = adapter.findLatestCreatedAtByMemberId(memberId)

                        latestCreatedAt.shouldNotBeNull()
                        // 최근 생성된 토큰의 시간과 근접해야 함
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

                        // 만료된 토큰 직접 생성
                        val expiredEntity = EmailVerificationTokenEntity(
                            id = "expired-token-1",
                            tokenHash = "expired-hash-1",
                            memberId = memberId.value,
                            expiresAt = now.minusHours(1), // 1시간 전 만료
                            createdAt = now.minusDays(2),
                        )
                        repository.save(expiredEntity)

                        // 유효한 토큰 생성
                        val validToken = adapter.create(MemberId.new())

                        // 만료된 토큰 삭제
                        val deletedCount = adapter.deleteExpiredTokens()

                        deletedCount shouldBe 1

                        // 만료된 토큰은 삭제됨
                        repository.findById("expired-token-1").isEmpty shouldBe true

                        // 유효한 토큰은 유지됨
                        repository.findById(validToken.info.id).isPresent shouldBe true
                    }
                }

                context("만료된 토큰이 없을 때") {
                    it("0을 반환한다") {
                        // 유효한 토큰만 생성
                        adapter.create(MemberId.new())

                        val deletedCount = adapter.deleteExpiredTokens()

                        deletedCount shouldBe 0
                    }
                }
            }
        }
    }
}

// region Test Utilities

/**
 * SHA-256 해시 생성 (어댑터와 동일한 로직)
 */
private fun hashToken(token: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}

// endregion
