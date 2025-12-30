package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EncryptionAlreadySetupException
import io.clroot.selah.domains.member.domain.exception.EncryptionSettingsNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.springframework.context.ApplicationEventPublisher
import java.util.Base64

class EncryptionSettingsServiceTest : DescribeSpec({

    val loadEncryptionSettingsPort = mockk<LoadEncryptionSettingsPort>()
    val saveEncryptionSettingsPort = mockk<SaveEncryptionSettingsPort>()
    val deleteEncryptionSettingsPort = mockk<DeleteEncryptionSettingsPort>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val encryptionSettingsService = EncryptionSettingsService(
        loadEncryptionSettingsPort = loadEncryptionSettingsPort,
        saveEncryptionSettingsPort = saveEncryptionSettingsPort,
        deleteEncryptionSettingsPort = deleteEncryptionSettingsPort,
        eventPublisher = eventPublisher,
    )

    beforeTest {
        clearMocks(
            loadEncryptionSettingsPort,
            saveEncryptionSettingsPort,
            deleteEncryptionSettingsPort,
            eventPublisher,
        )
    }

    describe("setup") {
        val memberId = MemberId.new()
        val salt = Base64.getEncoder().encodeToString("test-salt-16bytes".toByteArray())
        val recoveryKeyHash = "hashed-recovery-key"
        val command = SetupEncryptionCommand(
            salt = salt,
            recoveryKeyHash = recoveryKeyHash,
        )

        context("암호화 설정이 없는 경우") {

            it("새 암호화 설정이 생성되고 저장된다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns false
                coEvery { saveEncryptionSettingsPort.save(any()) } answers { firstArg() }

                val result = encryptionSettingsService.setup(memberId, command)

                result shouldNotBe null
                result.memberId shouldBe memberId
                result.salt shouldBe salt
                result.recoveryKeyHash shouldBe recoveryKeyHash
                result.isEnabled shouldBe true

                coVerify(exactly = 1) { loadEncryptionSettingsPort.existsByMemberId(memberId) }
                coVerify(exactly = 1) { saveEncryptionSettingsPort.save(any()) }
            }
        }

        context("이미 암호화 설정이 있는 경우") {

            it("EncryptionAlreadySetupException을 던진다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns true

                val exception = shouldThrow<EncryptionAlreadySetupException> {
                    encryptionSettingsService.setup(memberId, command)
                }

                exception.message shouldBe "Encryption already setup for member: ${memberId.value}"

                coVerify(exactly = 1) { loadEncryptionSettingsPort.existsByMemberId(memberId) }
                coVerify(exactly = 0) { saveEncryptionSettingsPort.save(any()) }
            }
        }
    }

    describe("getSettings") {
        val memberId = MemberId.new()

        context("암호화 설정이 존재하는 경우") {

            it("설정을 반환한다") {
                val existingSettings = createEncryptionSettings(memberId)
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings

                val result = encryptionSettingsService.getSettings(memberId)

                result shouldBe existingSettings
                coVerify(exactly = 1) { loadEncryptionSettingsPort.findByMemberId(memberId) }
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                val exception = shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.getSettings(memberId)
                }

                exception.message shouldBe "Encryption settings not found for member: ${memberId.value}"
            }
        }
    }

    describe("hasSettings") {
        val memberId = MemberId.new()

        context("암호화 설정이 존재하는 경우") {

            it("true를 반환한다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns true

                val result = encryptionSettingsService.hasSettings(memberId)

                result.shouldBeTrue()
            }
        }

        context("암호화 설정이 없는 경우") {

            it("false를 반환한다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns false

                val result = encryptionSettingsService.hasSettings(memberId)

                result.shouldBeFalse()
            }
        }
    }

    describe("verifyRecoveryKey") {
        val memberId = MemberId.new()
        val correctHash = "correct-recovery-key-hash"
        val wrongHash = "wrong-recovery-key-hash"

        context("암호화 설정이 존재하는 경우") {

            val existingSettings = createEncryptionSettings(
                memberId = memberId,
                recoveryKeyHash = correctHash,
            )

            it("올바른 복구 키 해시면 true를 반환한다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings

                val result = encryptionSettingsService.verifyRecoveryKey(memberId, correctHash)

                result.shouldBeTrue()
            }

            it("잘못된 복구 키 해시면 false를 반환한다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings

                val result = encryptionSettingsService.verifyRecoveryKey(memberId, wrongHash)

                result.shouldBeFalse()
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.verifyRecoveryKey(memberId, correctHash)
                }
            }
        }
    }

    describe("deleteSettings") {
        val memberId = MemberId.new()

        context("암호화 설정이 존재하는 경우") {

            it("설정을 삭제하고 이벤트를 발행한다") {
                val existingSettings = createEncryptionSettings(memberId)
                val eventSlot = slot<Any>()
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings
                coEvery { deleteEncryptionSettingsPort.deleteByMemberId(memberId) } just Runs
                every { eventPublisher.publishEvent(capture(eventSlot)) } just Runs

                encryptionSettingsService.deleteSettings(memberId)

                coVerify(exactly = 1) { loadEncryptionSettingsPort.findByMemberId(memberId) }
                coVerify(exactly = 1) { deleteEncryptionSettingsPort.deleteByMemberId(memberId) }

                // 발행된 이벤트 검증
                eventSlot.isCaptured shouldBe true
                val capturedEvent = eventSlot.captured
                capturedEvent::class.simpleName shouldBe "EncryptionSettingsDeletedEvent"
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.deleteSettings(memberId)
                }

                coVerify(exactly = 0) { deleteEncryptionSettingsPort.deleteByMemberId(memberId) }
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
