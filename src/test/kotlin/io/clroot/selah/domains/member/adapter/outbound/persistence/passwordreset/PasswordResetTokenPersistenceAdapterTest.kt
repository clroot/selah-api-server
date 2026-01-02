package io.clroot.selah.domains.member.adapter.outbound.persistence.passwordreset

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@SpringBootTest
@TestPropertySource(
    properties = [
        "selah.password-reset.ttl=PT1H",
    ],
)
class PasswordResetTokenPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: PasswordResetTokenPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("PasswordResetTokenPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("create") {
                context("새 비밀번호 재설정 토큰을 생성할 때") {
                    it("토큰이 생성되고 원본 토큰이 반환된다") {
                        // When
                        val result = adapter.create(testMember.id)

                        // Then
                        result.shouldNotBeNull()
                        result.rawToken.shouldNotBeNull()
                        result.rawToken.length shouldBe 64
                        result.info.shouldNotBeNull()
                        result.info.memberId shouldBe testMember.id
                        result.info.isValid().shouldBeTrue()
                        result.info.isUsed().shouldBeFalse()
                        result.info.isExpired().shouldBeFalse()
                    }
                }

                context("같은 회원이 여러 토큰을 생성할 때") {
                    it("각각 다른 토큰이 생성된다") {
                        // When
                        val result1 = adapter.create(testMember.id)
                        val result2 = adapter.create(testMember.id)

                        // Then
                        result1.rawToken shouldNotBe result2.rawToken
                        result1.info.id shouldNotBe result2.info.id
                    }
                }
            }

            describe("findValidByToken") {
                context("유효한 토큰으로 조회할 때") {
                    it("토큰 정보를 반환한다") {
                        // Given
                        val created = adapter.create(testMember.id)

                        // When
                        val found = adapter.findValidByToken(created.rawToken)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe created.info.id
                        found.memberId shouldBe testMember.id
                        found.isValid().shouldBeTrue()
                    }
                }

                context("사용된 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        // Given
                        val created = adapter.create(testMember.id)
                        adapter.markAsUsed(created.info.id)

                        // When
                        val found = adapter.findValidByToken(created.rawToken)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("존재하지 않는 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findValidByToken("0".repeat(64))

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("잘못된 토큰으로 조회할 때") {
                    it("null을 반환한다") {
                        // Given
                        adapter.create(testMember.id)

                        // When
                        val found = adapter.findValidByToken("wrong-token")

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("markAsUsed") {
                context("토큰을 사용 처리할 때") {
                    it("usedAt이 설정되고 토큰이 무효화된다") {
                        // Given
                        val created = adapter.create(testMember.id)

                        // When
                        adapter.markAsUsed(created.info.id)

                        // Then
                        val found = adapter.findValidByToken(created.rawToken)
                        found.shouldBeNull()
                    }
                }

                context("이미 사용된 토큰을 다시 사용 처리할 때") {
                    it("오류 없이 처리된다") {
                        // Given
                        val created = adapter.create(testMember.id)
                        adapter.markAsUsed(created.info.id)

                        // When & Then
                        adapter.markAsUsed(created.info.id)
                    }
                }

                context("존재하지 않는 토큰을 사용 처리할 때") {
                    it("오류 없이 무시된다") {
                        // When & Then
                        adapter.markAsUsed("01HZXYZ1234567890ABCDEFGHI")
                    }
                }
            }

            describe("invalidateAllByMemberId") {
                context("회원의 모든 토큰을 무효화할 때") {
                    it("모든 토큰이 삭제된다") {
                        // Given
                        val token1 = adapter.create(testMember.id)
                        val token2 = adapter.create(testMember.id)
                        val token3 = adapter.create(testMember.id)

                        // When
                        adapter.invalidateAllByMemberId(testMember.id)

                        // Then
                        adapter.findValidByToken(token1.rawToken).shouldBeNull()
                        adapter.findValidByToken(token2.rawToken).shouldBeNull()
                        adapter.findValidByToken(token3.rawToken).shouldBeNull()
                    }
                }

                context("다른 회원의 토큰은 삭제되지 않을 때") {
                    it("대상 회원의 토큰만 삭제된다") {
                        // Given
                        val otherMember = createAndSaveMember()

                        val targetToken = adapter.create(testMember.id)
                        val otherToken = adapter.create(otherMember.id)

                        // When
                        adapter.invalidateAllByMemberId(testMember.id)

                        // Then
                        adapter.findValidByToken(targetToken.rawToken).shouldBeNull()
                        adapter.findValidByToken(otherToken.rawToken).shouldNotBeNull()
                    }
                }

                context("토큰이 없는 회원의 토큰을 무효화할 때") {
                    it("오류 없이 처리된다") {
                        // Given
                        val emptyMember = createAndSaveMember()

                        // When & Then
                        adapter.invalidateAllByMemberId(emptyMember.id)
                    }
                }
            }

            describe("findLatestCreatedAtByMemberId") {
                context("토큰이 존재할 때") {
                    it("가장 최근 토큰 생성 시간을 반환한다") {
                        // Given
                        adapter.create(testMember.id)
                        Thread.sleep(10)
                        val latest = adapter.create(testMember.id)

                        // When
                        val createdAt = adapter.findLatestCreatedAtByMemberId(testMember.id)

                        // Then
                        createdAt.shouldNotBeNull()
                        createdAt shouldBe latest.info.createdAt
                    }
                }

                context("여러 회원의 토큰이 있을 때") {
                    it("해당 회원의 최근 시간만 반환한다") {
                        // Given
                        val otherMember = createAndSaveMember()

                        adapter.create(otherMember.id)
                        Thread.sleep(10)
                        val targetToken = adapter.create(testMember.id)

                        // When
                        val createdAt = adapter.findLatestCreatedAtByMemberId(testMember.id)

                        // Then
                        createdAt.shouldNotBeNull()
                        createdAt shouldBe targetToken.info.createdAt
                    }
                }

                context("토큰이 없을 때") {
                    it("null을 반환한다") {
                        // Given
                        val emptyMember = createAndSaveMember()

                        // When
                        val createdAt = adapter.findLatestCreatedAtByMemberId(emptyMember.id)

                        // Then
                        createdAt.shouldBeNull()
                    }
                }
            }

            describe("deleteExpiredTokens") {
                context("만료된 토큰이 없을 때") {
                    it("0을 반환한다") {
                        // Given
                        adapter.create(testMember.id)

                        // When
                        val deletedCount = adapter.deleteExpiredTokens()

                        // Then
                        deletedCount shouldBe 0
                    }
                }

                context("만료되지 않은 토큰은 삭제되지 않을 때") {
                    it("유효한 토큰은 유지된다") {
                        // Given
                        val validToken = adapter.create(testMember.id)

                        // When
                        adapter.deleteExpiredTokens()

                        // Then
                        val found = adapter.findValidByToken(validToken.rawToken)
                        found.shouldNotBeNull()
                    }
                }
            }

            describe("PasswordResetTokenInfo") {
                context("isExpired") {
                    it("만료 여부를 정확히 판단한다") {
                        // Given
                        val created = adapter.create(testMember.id)

                        // When & Then
                        created.info.isExpired().shouldBeFalse()

                        val expired = created.info.copy(expiresAt = LocalDateTime.now().minusHours(1))
                        expired.isExpired().shouldBeTrue()
                    }
                }

                context("isUsed") {
                    it("사용 여부를 정확히 판단한다") {
                        // Given
                        val created = adapter.create(testMember.id)

                        // When & Then
                        created.info.isUsed().shouldBeFalse()

                        val used = created.info.copy(usedAt = LocalDateTime.now())
                        used.isUsed().shouldBeTrue()
                    }
                }

                context("isValid") {
                    it("유효성을 정확히 판단한다") {
                        // Given
                        val created = adapter.create(testMember.id)

                        // When & Then
                        created.info.isValid().shouldBeTrue()

                        val expired = created.info.copy(expiresAt = LocalDateTime.now().minusHours(1))
                        expired.isValid().shouldBeFalse()

                        val used = created.info.copy(usedAt = LocalDateTime.now())
                        used.isValid().shouldBeFalse()
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member = Member.createWithEmail(
            email = Email("password-reset-test-${System.currentTimeMillis()}@example.com"),
            nickname = "비밀번호 재설정 테스트",
            passwordHash = PasswordHash("hashed-password"),
        )
        return memberAdapter.save(member)
    }
}
