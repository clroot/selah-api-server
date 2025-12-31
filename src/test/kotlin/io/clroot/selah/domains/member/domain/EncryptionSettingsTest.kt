package io.clroot.selah.domains.member.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import java.util.Base64

class EncryptionSettingsTest : DescribeSpec({

    describe("EncryptionSettings 생성") {

        context("유효한 정보로 생성할 때") {
            val memberId = MemberId.new()
            val salt = Base64.getEncoder().encodeToString("test-salt-16bytes".toByteArray())
            val encryptedDEK = Base64.getEncoder().encodeToString("encrypted-dek-value".toByteArray())
            val recoveryEncryptedDEK = Base64.getEncoder().encodeToString("recovery-encrypted-dek".toByteArray())
            val recoveryKeyHash = "hashed-recovery-key-value"

            it("EncryptionSettings가 생성된다") {
                val settings = EncryptionSettings.create(
                    memberId = memberId,
                    salt = salt,
                    encryptedDEK = encryptedDEK,
                    recoveryEncryptedDEK = recoveryEncryptedDEK,
                    recoveryKeyHash = recoveryKeyHash,
                )

                settings.id shouldNotBe null
                settings.memberId shouldBe memberId
                settings.salt shouldBe salt
                settings.encryptedDEK shouldBe encryptedDEK
                settings.recoveryEncryptedDEK shouldBe recoveryEncryptedDEK
                settings.recoveryKeyHash shouldBe recoveryKeyHash
            }

            it("생성 시점이 설정된다") {
                val before = LocalDateTime.now()
                val settings = EncryptionSettings.create(
                    memberId = memberId,
                    salt = salt,
                    encryptedDEK = encryptedDEK,
                    recoveryEncryptedDEK = recoveryEncryptedDEK,
                    recoveryKeyHash = recoveryKeyHash,
                )
                val after = LocalDateTime.now()

                settings.createdAt shouldNotBe null
                settings.createdAt.isAfter(before.minusSeconds(1)).shouldBeTrue()
                settings.createdAt.isBefore(after.plusSeconds(1)).shouldBeTrue()
            }

            it("version은 null로 시작한다") {
                val settings = EncryptionSettings.create(
                    memberId = memberId,
                    salt = salt,
                    encryptedDEK = encryptedDEK,
                    recoveryEncryptedDEK = recoveryEncryptedDEK,
                    recoveryKeyHash = recoveryKeyHash,
                )

                settings.version shouldBe null
            }
        }

        context("불변식 위반") {

            it("salt가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "",
                        encryptedDEK = "valid-encrypted-dek",
                        recoveryEncryptedDEK = "valid-recovery-encrypted-dek",
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("salt가 공백만 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "   ",
                        encryptedDEK = "valid-encrypted-dek",
                        recoveryEncryptedDEK = "valid-recovery-encrypted-dek",
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("encryptedDEK가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "valid-salt",
                        encryptedDEK = "",
                        recoveryEncryptedDEK = "valid-recovery-encrypted-dek",
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("recoveryEncryptedDEK가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "valid-salt",
                        encryptedDEK = "valid-encrypted-dek",
                        recoveryEncryptedDEK = "",
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("recoveryKeyHash가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "valid-salt",
                        encryptedDEK = "valid-encrypted-dek",
                        recoveryEncryptedDEK = "valid-recovery-encrypted-dek",
                        recoveryKeyHash = "",
                    )
                }
            }
        }
    }

    describe("암호화 키 업데이트 (비밀번호 변경 시)") {

        it("새로운 Salt와 encryptedDEK로 업데이트할 수 있다") {
            val settings = createEncryptionSettings()
            val newSalt = "new-salt-value"
            val newEncryptedDEK = "new-encrypted-dek-value"

            settings.updateEncryption(newSalt, newEncryptedDEK)

            settings.salt shouldBe newSalt
            settings.encryptedDEK shouldBe newEncryptedDEK
        }

        it("Salt가 빈 문자열이면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateEncryption("", "valid-encrypted-dek")
            }
        }

        it("encryptedDEK가 빈 문자열이면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateEncryption("valid-salt", "")
            }
        }
    }

    describe("복구 키 업데이트") {

        it("새로운 recoveryEncryptedDEK와 recoveryKeyHash로 업데이트할 수 있다") {
            val settings = createEncryptionSettings()
            val newRecoveryEncryptedDEK = "new-recovery-encrypted-dek"
            val newRecoveryKeyHash = "new-recovery-key-hash"

            settings.updateRecoveryKey(newRecoveryEncryptedDEK, newRecoveryKeyHash)

            settings.recoveryEncryptedDEK shouldBe newRecoveryEncryptedDEK
            settings.recoveryKeyHash shouldBe newRecoveryKeyHash
        }

        it("recoveryEncryptedDEK가 빈 문자열이면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateRecoveryKey("", "valid-hash")
            }
        }

        it("recoveryKeyHash가 빈 문자열이면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateRecoveryKey("valid-recovery-encrypted-dek", "")
            }
        }
    }

})

// region Test Fixtures

private fun createEncryptionSettings(
    memberId: MemberId = MemberId.new(),
    salt: String = Base64.getEncoder().encodeToString("test-salt-16bytes".toByteArray()),
    encryptedDEK: String = Base64.getEncoder().encodeToString("test-encrypted-dek".toByteArray()),
    recoveryEncryptedDEK: String = Base64.getEncoder().encodeToString("test-recovery-encrypted-dek".toByteArray()),
    recoveryKeyHash: String = "test-recovery-key-hash",
): EncryptionSettings {
    return EncryptionSettings.create(
        memberId = memberId,
        salt = salt,
        encryptedDEK = encryptedDEK,
        recoveryEncryptedDEK = recoveryEncryptedDEK,
        recoveryKeyHash = recoveryKeyHash,
    )
}

// endregion
