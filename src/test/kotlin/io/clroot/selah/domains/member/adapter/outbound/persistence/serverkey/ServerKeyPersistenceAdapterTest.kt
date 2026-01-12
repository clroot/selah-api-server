package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.member.domain.ServerKey
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ServerKeyPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: ServerKeyPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("ServerKeyPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("save") {
                context("새 서버 키를 저장할 때") {
                    it("저장된 서버 키를 반환한다") {
                        // Given
                        val serverKey =
                            ServerKey.create(
                                memberId = testMember.id,
                                encryptedServerKey = "encrypted-server-key-base64",
                                iv = "iv-base64",
                            )

                        // When
                        val saved = adapter.save(serverKey)

                        // Then
                        saved.id shouldBe serverKey.id
                        saved.memberId shouldBe testMember.id
                        saved.encryptedServerKey shouldBe "encrypted-server-key-base64"
                        saved.iv shouldBe "iv-base64"
                    }
                }

                context("기존 서버 키를 업데이트할 때") {
                    it("변경사항이 저장된다") {
                        // Given
                        val serverKey = createAndSaveServerKey(testMember.id)
                        serverKey.updateServerKey(
                            newEncryptedServerKey = "new-encrypted-server-key",
                            newIv = "new-iv",
                        )

                        // When
                        val updated = adapter.save(serverKey)

                        // Then
                        updated.id shouldBe serverKey.id
                        updated.encryptedServerKey shouldBe "new-encrypted-server-key"
                        updated.iv shouldBe "new-iv"
                    }
                }
            }

            describe("findByMemberId") {
                context("회원의 서버 키가 존재할 때") {
                    it("서버 키를 반환한다") {
                        // Given
                        val serverKey = createAndSaveServerKey(testMember.id)

                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe serverKey.id
                        found.memberId shouldBe testMember.id
                        found.encryptedServerKey shouldBe serverKey.encryptedServerKey
                        found.iv shouldBe serverKey.iv
                    }
                }

                context("서버 키가 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("다른 회원의 서버 키는") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        createAndSaveServerKey(otherMember.id)

                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("existsByMemberId") {
                context("회원의 서버 키가 존재할 때") {
                    it("true를 반환한다") {
                        // Given
                        createAndSaveServerKey(testMember.id)

                        // When
                        val exists = adapter.existsByMemberId(testMember.id)

                        // Then
                        exists.shouldBeTrue()
                    }
                }

                context("서버 키가 존재하지 않을 때") {
                    it("false를 반환한다") {
                        // When
                        val exists = adapter.existsByMemberId(testMember.id)

                        // Then
                        exists.shouldBeFalse()
                    }
                }
            }

            describe("deleteByMemberId") {
                context("서버 키가 존재할 때") {
                    it("삭제된다") {
                        // Given
                        createAndSaveServerKey(testMember.id)

                        // When
                        adapter.deleteByMemberId(testMember.id)

                        // Then
                        val found = adapter.findByMemberId(testMember.id)
                        found.shouldBeNull()
                    }
                }

                context("서버 키가 존재하지 않을 때") {
                    it("에러 없이 정상 처리된다") {
                        // When & Then
                        adapter.deleteByMemberId(testMember.id)
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member =
            Member.createWithEmail(
                email = Email("test-${System.currentTimeMillis()}@example.com"),
                nickname = "테스트 사용자",
                passwordHash = PasswordHash("hashed-password"),
            )
        return memberAdapter.save(member)
    }

    private suspend fun createAndSaveServerKey(memberId: MemberId): ServerKey {
        val serverKey =
            ServerKey.create(
                memberId = memberId,
                encryptedServerKey = "encrypted-server-key-${System.currentTimeMillis()}",
                iv = "iv-${System.currentTimeMillis()}",
            )
        return adapter.save(serverKey)
    }
}
