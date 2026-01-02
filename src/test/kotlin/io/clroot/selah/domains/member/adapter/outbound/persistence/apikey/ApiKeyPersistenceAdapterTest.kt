package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(
    properties = [
        "selah.api-key.prefix=selah_",
    ],
)
class ApiKeyPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: ApiKeyPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("ApiKeyPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("create") {
                context("새 API Key를 생성할 때") {
                    it("API Key가 생성되고 원본 키가 반환된다") {
                        // When
                        val result = adapter.create(
                            memberId = testMember.id,
                            role = testMember.role,
                            name = "테스트 API Key",
                            ipAddress = "127.0.0.1",
                        )

                        // Then
                        result.shouldNotBeNull()
                        result.rawKey.shouldNotBeNull()
                        result.rawKey shouldStartWith "selah_"
                        result.info.shouldNotBeNull()
                        result.info.memberId shouldBe testMember.id
                        result.info.role shouldBe Member.Role.USER
                        result.info.name shouldBe "테스트 API Key"
                        result.info.prefix shouldStartWith "selah_"
                        result.info.createdIp shouldBe "127.0.0.1"
                        result.info.lastUsedAt.shouldBeNull()
                        result.info.lastUsedIp.shouldBeNull()
                    }
                }

                context("같은 회원이 여러 API Key를 생성할 때") {
                    it("각각 다른 키가 생성된다") {
                        // When
                        val result1 = adapter.create(testMember.id, testMember.role, "Key 1", null)
                        val result2 = adapter.create(testMember.id, testMember.role, "Key 2", null)

                        // Then
                        result1.rawKey shouldNotBe result2.rawKey
                        result1.info.id shouldNotBe result2.info.id
                        result1.info.name shouldBe "Key 1"
                        result2.info.name shouldBe "Key 2"
                    }
                }

                context("IP 주소 없이 API Key를 생성할 때") {
                    it("null IP로 키가 생성된다") {
                        // When
                        val result = adapter.create(
                            memberId = testMember.id,
                            role = testMember.role,
                            name = "No IP Key",
                            ipAddress = null,
                        )

                        // Then
                        result.info.createdIp.shouldBeNull()
                    }
                }
            }

            describe("findByKey") {
                context("존재하는 API Key로 조회할 때") {
                    it("API Key 정보를 반환한다") {
                        // Given
                        val created = adapter.create(testMember.id, testMember.role, "조회 테스트", "10.0.0.1")

                        // When
                        val found = adapter.findByKey(created.rawKey)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe created.info.id
                        found.memberId shouldBe testMember.id
                        found.name shouldBe "조회 테스트"
                    }
                }

                context("존재하지 않는 API Key로 조회할 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByKey("selah_" + "0".repeat(64))

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("잘못된 형식의 키로 조회할 때") {
                    it("null을 반환한다") {
                        // Given
                        adapter.create(testMember.id, testMember.role, "Valid Key", null)

                        // When
                        val found = adapter.findByKey("invalid-key")

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("delete") {
                context("API Key를 삭제할 때") {
                    it("키가 삭제된다") {
                        // Given
                        val created = adapter.create(testMember.id, testMember.role, "삭제 대상", null)

                        // When
                        adapter.delete(created.info.id)

                        // Then
                        val found = adapter.findByKey(created.rawKey)
                        found.shouldBeNull()
                    }
                }

                context("존재하지 않는 API Key를 삭제할 때") {
                    it("오류 없이 무시된다") {
                        // When & Then
                        adapter.delete("01HZXYZ1234567890ABCDEFGHI")
                    }
                }
            }

            describe("findAllByMemberId") {
                context("회원의 모든 API Key를 조회할 때") {
                    it("모든 키를 반환한다") {
                        // Given
                        val key1 = adapter.create(testMember.id, testMember.role, "Key 1", null)
                        val key2 = adapter.create(testMember.id, testMember.role, "Key 2", null)
                        val key3 = adapter.create(testMember.id, testMember.role, "Key 3", null)

                        // When
                        val keys = adapter.findAllByMemberId(testMember.id)

                        // Then
                        keys shouldHaveSize 3
                        keys.map { it.id } shouldContainExactlyInAnyOrder listOf(
                            key1.info.id,
                            key2.info.id,
                            key3.info.id,
                        )
                    }
                }

                context("다른 회원의 키는 조회되지 않을 때") {
                    it("해당 회원의 키만 반환한다") {
                        // Given
                        val otherMember = createAndSaveMember()

                        adapter.create(testMember.id, testMember.role, "My Key", null)
                        adapter.create(otherMember.id, otherMember.role, "Other Key", null)

                        // When
                        val keys = adapter.findAllByMemberId(testMember.id)

                        // Then
                        keys shouldHaveSize 1
                        keys.first().name shouldBe "My Key"
                    }
                }

                context("API Key가 없는 회원을 조회할 때") {
                    it("빈 리스트를 반환한다") {
                        // Given
                        val emptyMember = createAndSaveMember()

                        // When
                        val keys = adapter.findAllByMemberId(emptyMember.id)

                        // Then
                        keys.shouldBeEmpty()
                    }
                }
            }

            describe("updateLastUsedAt") {
                context("API Key 사용 기록을 업데이트할 때") {
                    it("lastUsedAt과 lastUsedIp가 업데이트된다") {
                        // Given
                        val created = adapter.create(testMember.id, testMember.role, "사용 추적", "127.0.0.1")
                        created.info.lastUsedAt.shouldBeNull()

                        // When
                        adapter.updateLastUsedAt(created.info.id, "192.168.1.1")

                        // Then
                        val updated = adapter.findByKey(created.rawKey)
                        updated.shouldNotBeNull()
                        updated.lastUsedAt.shouldNotBeNull()
                        updated.lastUsedIp shouldBe "192.168.1.1"
                    }
                }

                context("여러 번 사용할 때") {
                    it("가장 최근 사용 정보가 유지된다") {
                        // Given
                        val created = adapter.create(testMember.id, testMember.role, "다중 사용", null)

                        // When
                        adapter.updateLastUsedAt(created.info.id, "10.0.0.1")
                        val firstUpdate = adapter.findByKey(created.rawKey)

                        Thread.sleep(10)
                        adapter.updateLastUsedAt(created.info.id, "10.0.0.2")
                        val secondUpdate = adapter.findByKey(created.rawKey)

                        // Then
                        firstUpdate.shouldNotBeNull()
                        secondUpdate.shouldNotBeNull()
                        secondUpdate.lastUsedAt!! > firstUpdate.lastUsedAt!!
                        secondUpdate.lastUsedIp shouldBe "10.0.0.2"
                    }
                }

                context("IP 주소 없이 업데이트할 때") {
                    it("lastUsedAt만 업데이트된다") {
                        // Given
                        val created = adapter.create(testMember.id, testMember.role, "IP 없음", null)

                        // When
                        adapter.updateLastUsedAt(created.info.id, null)

                        // Then
                        val updated = adapter.findByKey(created.rawKey)
                        updated.shouldNotBeNull()
                        updated.lastUsedAt.shouldNotBeNull()
                        updated.lastUsedIp.shouldBeNull()
                    }
                }

                context("존재하지 않는 API Key를 업데이트할 때") {
                    it("오류 없이 무시된다") {
                        // When & Then
                        adapter.updateLastUsedAt("01HZXYZ1234567890ABCDEFGHI", "127.0.0.1")
                    }
                }
            }

            describe("API Key 통합 시나리오") {
                context("API Key 생성부터 삭제까지") {
                    it("전체 생명주기가 정상 작동한다") {
                        // Given - Create
                        val created = adapter.create(
                            memberId = testMember.id,
                            role = testMember.role,
                            name = "Production API Key",
                            ipAddress = "203.0.113.1",
                        )

                        // When - Find
                        val found = adapter.findByKey(created.rawKey)
                        found.shouldNotBeNull()

                        // When - Update Usage
                        adapter.updateLastUsedAt(created.info.id, "198.51.100.1")
                        val updated = adapter.findByKey(created.rawKey)
                        updated.shouldNotBeNull()
                        updated.lastUsedAt.shouldNotBeNull()

                        // When - List
                        val allKeys = adapter.findAllByMemberId(testMember.id)
                        allKeys shouldHaveSize 1

                        // When - Delete
                        adapter.delete(created.info.id)
                        val deleted = adapter.findByKey(created.rawKey)
                        deleted.shouldBeNull()
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member = Member.createWithEmail(
            email = Email("apikey-test-${System.currentTimeMillis()}@example.com"),
            nickname = "API Key 테스트",
            passwordHash = PasswordHash("hashed-password"),
        )
        return memberAdapter.save(member)
    }
}
