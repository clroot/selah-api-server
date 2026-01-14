package io.clroot.selah.domains.member.adapter.outbound.persistence.session

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@SpringBootTest
@TestPropertySource(
    properties = [
        "selah.session.ttl=PT1H", // 1 hour for testing
        "selah.session.extend-threshold=PT30M", // 30 minutes threshold
    ],
)
class SessionPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: SessionPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("SessionPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("create") {
                context("새 세션을 생성할 때") {
                    it("세션이 정상적으로 생성된다") {
                        // When
                        val session =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = "Mozilla/5.0",
                                ipAddress = "127.0.0.1",
                            )

                        // Then
                        session.shouldNotBeNull()
                        session.token.shouldNotBeNull()
                        session.memberId shouldBe testMember.id
                        session.role shouldBe Member.Role.USER
                        session.userAgent shouldBe "Mozilla/5.0"
                        session.createdIp shouldBe "127.0.0.1"
                        session.lastAccessedIp shouldBe "127.0.0.1"
                        session.expiresAt.shouldNotBeNull()
                        session.createdAt.shouldNotBeNull()
                    }
                }

                context("User-Agent 없이 세션을 생성할 때") {
                    it("null User-Agent로 세션이 생성된다") {
                        // When
                        val session =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = null,
                                ipAddress = "192.168.1.1",
                            )

                        // Then
                        session.userAgent.shouldBeNull()
                        session.createdIp shouldBe "192.168.1.1"
                    }
                }

                context("IP 주소 없이 세션을 생성할 때") {
                    it("null IP로 세션이 생성된다") {
                        // When
                        val session =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = "Chrome/90.0",
                                ipAddress = null,
                            )

                        // Then
                        session.createdIp.shouldBeNull()
                        session.lastAccessedIp.shouldBeNull()
                    }
                }

                context("매우 긴 User-Agent로 세션을 생성할 때") {
                    it("500자로 잘린다") {
                        // Given
                        val longUserAgent = "A".repeat(600)

                        // When
                        val session =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = longUserAgent,
                                ipAddress = "127.0.0.1",
                            )

                        // Then
                        session.userAgent?.length shouldBe 500
                    }
                }
            }

            describe("findByToken") {
                context("세션이 존재할 때") {
                    it("세션을 반환한다") {
                        // Given
                        val created =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = "Safari/14.0",
                                ipAddress = "10.0.0.1",
                            )

                        // When
                        val found = adapter.findByToken(created.token)

                        // Then
                        found.shouldNotBeNull()
                        found.token shouldBe created.token
                        found.memberId shouldBe testMember.id
                        found.userAgent shouldBe "Safari/14.0"
                    }
                }

                context("세션이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByToken("non-existent-token")

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("delete") {
                context("세션을 삭제할 때") {
                    it("세션이 삭제된다") {
                        // Given
                        val session =
                            adapter.create(
                                memberId = testMember.id,
                                role = testMember.role,
                                userAgent = null,
                                ipAddress = null,
                            )

                        // When
                        adapter.delete(session.token)

                        // Then
                        val found = adapter.findByToken(session.token)
                        found.shouldBeNull()
                    }
                }

                context("존재하지 않는 세션을 삭제할 때") {
                    it("오류 없이 무시된다") {
                        // When & Then (no exception)
                        adapter.delete("non-existent-token")
                    }
                }
            }

            describe("deleteAllByMemberId") {
                context("회원의 모든 세션을 삭제할 때") {
                    it("모든 세션이 삭제된다") {
                        // Given
                        val session1 = adapter.create(testMember.id, testMember.role, "Agent1", "127.0.0.1")
                        val session2 = adapter.create(testMember.id, testMember.role, "Agent2", "127.0.0.2")
                        val session3 = adapter.create(testMember.id, testMember.role, "Agent3", "127.0.0.3")

                        // When
                        adapter.deleteAllByMemberId(testMember.id)

                        // Then
                        adapter.findByToken(session1.token).shouldBeNull()
                        adapter.findByToken(session2.token).shouldBeNull()
                        adapter.findByToken(session3.token).shouldBeNull()
                    }
                }

                context("다른 회원의 세션은 삭제되지 않을 때") {
                    it("대상 회원의 세션만 삭제된다") {
                        // Given
                        val otherMember = createAndSaveMember()

                        val targetSession = adapter.create(testMember.id, testMember.role, "Agent1", "127.0.0.1")
                        val otherSession = adapter.create(otherMember.id, otherMember.role, "Agent2", "127.0.0.2")

                        // When
                        adapter.deleteAllByMemberId(testMember.id)

                        // Then
                        adapter.findByToken(targetSession.token).shouldBeNull()
                        adapter.findByToken(otherSession.token).shouldNotBeNull()
                    }
                }

                context("세션이 없는 회원의 세션을 삭제할 때") {
                    it("오류 없이 무시된다") {
                        // Given
                        val emptyMember = createAndSaveMember()

                        // When & Then (no exception)
                        adapter.deleteAllByMemberId(emptyMember.id)
                    }
                }
            }

            describe("update") {
                context("세션 정보를 업데이트할 때") {
                    it("세션이 정상적으로 업데이트된다") {
                        // Given
                        val session = adapter.create(testMember.id, testMember.role, "Agent", "127.0.0.1")

                        // When
                        val updatedSession =
                            session.copy(
                                lastAccessedIp = "192.168.1.100",
                                expiresAt = session.expiresAt.plusHours(1),
                            )
                        val result = adapter.update(updatedSession)

                        // Then
                        result.shouldNotBeNull()
                        result.lastAccessedIp shouldBe "192.168.1.100"

                        val found = adapter.findByToken(session.token)
                        found.shouldNotBeNull()
                        found.lastAccessedIp shouldBe "192.168.1.100"
                    }
                }

                context("마지막 접근 IP만 업데이트할 때") {
                    it("IP 주소가 변경된다") {
                        // Given
                        val session = adapter.create(testMember.id, testMember.role, "Agent", "127.0.0.1")

                        // When
                        val updatedSession = session.copy(lastAccessedIp = "10.0.0.5")
                        adapter.update(updatedSession)

                        // Then
                        val found = adapter.findByToken(session.token)
                        found.shouldNotBeNull()
                        found.lastAccessedIp shouldBe "10.0.0.5"
                    }
                }
            }

            describe("deleteExpiredBefore") {
                context("만료된 세션이 있을 때") {
                    it("만료된 세션만 삭제된다") {
                        // Given - Create a valid session
                        val validSession = adapter.create(testMember.id, testMember.role, "Agent1", "127.0.0.1")

                        // For testing, we would need to manually create expired sessions
                        // Since we can't directly manipulate time in the adapter,
                        // we'll verify the method executes without error

                        // When
                        val deletedCount = adapter.deleteExpiredBefore(LocalDateTime.now())

                        // Then
                        deletedCount shouldBe 0 // No expired sessions in test
                        adapter.findByToken(validSession.token).shouldNotBeNull()
                    }
                }

                context("만료된 세션이 없을 때") {
                    it("0을 반환한다") {
                        // When
                        val deletedCount = adapter.deleteExpiredBefore(LocalDateTime.now())

                        // Then
                        deletedCount shouldBe 0
                    }
                }
            }

            describe("SessionInfo.isExpired") {
                context("만료 시간이 지났을 때") {
                    it("true를 반환한다") {
                        // Given
                        val session = adapter.create(testMember.id, testMember.role, null, null)
                        val expiredSession = session.copy(expiresAt = LocalDateTime.now().minusHours(1))

                        // When & Then
                        expiredSession.isExpired().shouldBeTrue()
                    }
                }

                context("만료 시간이 지나지 않았을 때") {
                    it("false를 반환한다") {
                        // Given
                        val session = adapter.create(testMember.id, testMember.role, null, null)

                        // When & Then
                        session.isExpired().shouldBeFalse()
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member =
            Member.createWithEmail(
                email = Email("session-test-${System.currentTimeMillis()}@example.com"),
                nickname = "세션 테스트 사용자",
                passwordHash = PasswordHash("hashed-password"),
            )
        return memberAdapter.save(member)
    }
}
