package io.clroot.selah.domains.member.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
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
            val recoveryKeyHash = "hashed-recovery-key-value"

            it("EncryptionSettings가 생성된다") {
                val settings = EncryptionSettings.create(
                    memberId = memberId,
                    salt = salt,
                    recoveryKeyHash = recoveryKeyHash,
                )

                settings.id shouldNotBe null
                settings.memberId shouldBe memberId
                settings.salt shouldBe salt
                settings.recoveryKeyHash shouldBe recoveryKeyHash
                settings.isEnabled.shouldBeTrue()
            }

            it("생성 시점이 설정된다") {
                val before = LocalDateTime.now()
                val settings = EncryptionSettings.create(
                    memberId = memberId,
                    salt = salt,
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
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("salt가 공백만 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "   ",
                        recoveryKeyHash = "valid-hash",
                    )
                }
            }

            it("recoveryKeyHash가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "valid-salt",
                        recoveryKeyHash = "",
                    )
                }
            }

            it("recoveryKeyHash가 공백만 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    EncryptionSettings.create(
                        memberId = MemberId.new(),
                        salt = "valid-salt",
                        recoveryKeyHash = "   ",
                    )
                }
            }
        }
    }

    describe("Salt 업데이트") {

        it("새로운 Salt로 업데이트할 수 있다") {
            val settings = createEncryptionSettings()
            val newSalt = "new-salt-value"

            settings.updateSalt(newSalt)

            settings.salt shouldBe newSalt
        }

        it("빈 문자열로 업데이트하면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateSalt("")
            }
        }

        it("공백만 있는 문자열로 업데이트하면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateSalt("   ")
            }
        }
    }

    describe("복구 키 해시 업데이트") {

        it("새로운 복구 키 해시로 업데이트할 수 있다") {
            val settings = createEncryptionSettings()
            val newHash = "new-recovery-key-hash"

            settings.updateRecoveryKeyHash(newHash)

            settings.recoveryKeyHash shouldBe newHash
        }

        it("빈 문자열로 업데이트하면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateRecoveryKeyHash("")
            }
        }

        it("공백만 있는 문자열로 업데이트하면 실패한다") {
            val settings = createEncryptionSettings()

            shouldThrow<IllegalArgumentException> {
                settings.updateRecoveryKeyHash("   ")
            }
        }
    }

    describe("암호화 활성화/비활성화") {

        context("활성화") {

            it("비활성화된 상태에서 활성화할 수 있다") {
                val settings = createEncryptionSettings()
                settings.disable()
                settings.isEnabled.shouldBeFalse()

                settings.enable()

                settings.isEnabled.shouldBeTrue()
            }

            it("이미 활성화된 상태에서 활성화해도 상태가 유지된다") {
                val settings = createEncryptionSettings()
                settings.isEnabled.shouldBeTrue()

                settings.enable()

                settings.isEnabled.shouldBeTrue()
            }
        }

        context("비활성화") {

            it("활성화된 상태에서 비활성화할 수 있다") {
                val settings = createEncryptionSettings()
                settings.isEnabled.shouldBeTrue()

                settings.disable()

                settings.isEnabled.shouldBeFalse()
            }

            it("이미 비활성화된 상태에서 비활성화해도 상태가 유지된다") {
                val settings = createEncryptionSettings()
                settings.disable()
                settings.isEnabled.shouldBeFalse()

                settings.disable()

                settings.isEnabled.shouldBeFalse()
            }
        }
    }

})

// region Test Fixtures

private fun createEncryptionSettings(
    memberId: MemberId = MemberId.new(),
    salt: String = Base64.getEncoder().encodeToString("test-salt-16bytes".toByteArray()),
    recoveryKeyHash: String = "test-recovery-key-hash",
): EncryptionSettings {
    return EncryptionSettings.create(
        memberId = memberId,
        salt = salt,
        recoveryKeyHash = recoveryKeyHash,
    )
}

// endregion
