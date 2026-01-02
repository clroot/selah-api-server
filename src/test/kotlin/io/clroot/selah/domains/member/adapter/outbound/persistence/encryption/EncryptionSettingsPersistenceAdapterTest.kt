package io.clroot.selah.domains.member.adapter.outbound.persistence.encryption

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class EncryptionSettingsPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: EncryptionSettingsPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("EncryptionSettingsPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("save") {
                context("새 암호화 설정을 저장할 때") {
                    it("저장된 설정을 반환한다") {
                        // Given
                        val settings = EncryptionSettings.create(
                            memberId = testMember.id,
                            salt = "base64-salt",
                            encryptedDEK = "encrypted-dek",
                            recoveryEncryptedDEK = "recovery-encrypted-dek",
                            recoveryKeyHash = "recovery-key-hash",
                        )

                        // When
                        val saved = adapter.save(settings)

                        // Then
                        saved.id shouldBe settings.id
                        saved.memberId shouldBe testMember.id
                        saved.salt shouldBe "base64-salt"
                        saved.encryptedDEK shouldBe "encrypted-dek"
                        saved.recoveryEncryptedDEK shouldBe "recovery-encrypted-dek"
                        saved.recoveryKeyHash shouldBe "recovery-key-hash"
                    }
                }

                context("placeholder로 초기 설정을 저장할 때") {
                    it("PENDING 상태로 저장된다") {
                        // Given
                        val settings = EncryptionSettings.create(
                            memberId = testMember.id,
                            salt = "base64-salt",
                            encryptedDEK = null,
                            recoveryEncryptedDEK = "recovery-encrypted-dek",
                            recoveryKeyHash = "recovery-key-hash",
                        )

                        // When
                        val saved = adapter.save(settings)

                        // Then
                        saved.encryptedDEK shouldBe EncryptionSettings.PLACEHOLDER_ENCRYPTED_DEK
                    }
                }

                context("기존 설정을 업데이트할 때") {
                    it("변경사항이 저장된다") {
                        // Given
                        val settings = createAndSaveEncryptionSettings(testMember.id)
                        settings.updateEncryption(
                            newSalt = "new-salt",
                            newEncryptedDEK = "new-encrypted-dek",
                        )

                        // When
                        val updated = adapter.save(settings)

                        // Then
                        updated.id shouldBe settings.id
                        updated.salt shouldBe "new-salt"
                        updated.encryptedDEK shouldBe "new-encrypted-dek"
                    }
                }

                context("복구 키를 재생성할 때") {
                    it("복구 키 정보가 업데이트된다") {
                        // Given
                        val settings = createAndSaveEncryptionSettings(testMember.id)
                        settings.updateRecoveryKey(
                            newRecoveryEncryptedDEK = "new-recovery-encrypted-dek",
                            newRecoveryKeyHash = "new-recovery-key-hash",
                        )

                        // When
                        val updated = adapter.save(settings)

                        // Then
                        updated.recoveryEncryptedDEK shouldBe "new-recovery-encrypted-dek"
                        updated.recoveryKeyHash shouldBe "new-recovery-key-hash"
                    }
                }
            }

            describe("findByMemberId") {
                context("회원의 암호화 설정이 존재할 때") {
                    it("설정을 반환한다") {
                        // Given
                        val settings = createAndSaveEncryptionSettings(testMember.id)

                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe settings.id
                        found.memberId shouldBe testMember.id
                    }
                }

                context("암호화 설정이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("다른 회원의 설정은") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        createAndSaveEncryptionSettings(otherMember.id)

                        // When
                        val found = adapter.findByMemberId(testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("existsByMemberId") {
                context("회원의 암호화 설정이 존재할 때") {
                    it("true를 반환한다") {
                        // Given
                        createAndSaveEncryptionSettings(testMember.id)

                        // When
                        val exists = adapter.existsByMemberId(testMember.id)

                        // Then
                        exists.shouldBeTrue()
                    }
                }

                context("암호화 설정이 존재하지 않을 때") {
                    it("false를 반환한다") {
                        // When
                        val exists = adapter.existsByMemberId(testMember.id)

                        // Then
                        exists.shouldBeFalse()
                    }
                }
            }

            describe("deleteByMemberId") {
                context("암호화 설정이 존재할 때") {
                    it("삭제된다") {
                        // Given
                        createAndSaveEncryptionSettings(testMember.id)

                        // When
                        adapter.deleteByMemberId(testMember.id)

                        // Then
                        val found = adapter.findByMemberId(testMember.id)
                        found.shouldBeNull()
                    }
                }

                context("암호화 설정이 존재하지 않을 때") {
                    it("에러 없이 정상 처리된다") {
                        // When & Then
                        adapter.deleteByMemberId(testMember.id)
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member = Member.createWithEmail(
            email = Email("test-${System.currentTimeMillis()}@example.com"),
            nickname = "테스트 사용자",
            passwordHash = PasswordHash("hashed-password"),
        )
        return memberAdapter.save(member)
    }

    private suspend fun createAndSaveEncryptionSettings(memberId: MemberId): EncryptionSettings {
        val settings = EncryptionSettings.create(
            memberId = memberId,
            salt = "base64-salt-${System.currentTimeMillis()}",
            encryptedDEK = "encrypted-dek",
            recoveryEncryptedDEK = "recovery-encrypted-dek",
            recoveryKeyHash = "recovery-key-hash",
        )
        return adapter.save(settings)
    }
}
