package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateRecoveryKeyCommand
import io.clroot.selah.domains.member.application.port.outbound.DeleteEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.DeleteServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.EncryptedServerKeyResult
import io.clroot.selah.domains.member.application.port.outbound.LoadEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.LoadServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.SaveEncryptionSettingsPort
import io.clroot.selah.domains.member.application.port.outbound.SaveServerKeyPort
import io.clroot.selah.domains.member.application.port.outbound.ServerKeyEncryptionPort
import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey
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
    val loadServerKeyPort = mockk<LoadServerKeyPort>()
    val saveServerKeyPort = mockk<SaveServerKeyPort>()
    val deleteServerKeyPort = mockk<DeleteServerKeyPort>()
    val serverKeyEncryptionPort = mockk<ServerKeyEncryptionPort>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val encryptionSettingsService = EncryptionSettingsService(
        loadEncryptionSettingsPort = loadEncryptionSettingsPort,
        saveEncryptionSettingsPort = saveEncryptionSettingsPort,
        deleteEncryptionSettingsPort = deleteEncryptionSettingsPort,
        loadServerKeyPort = loadServerKeyPort,
        saveServerKeyPort = saveServerKeyPort,
        deleteServerKeyPort = deleteServerKeyPort,
        serverKeyEncryptionPort = serverKeyEncryptionPort,
        eventPublisher = eventPublisher,
    )

    beforeTest {
        clearMocks(
            loadEncryptionSettingsPort,
            saveEncryptionSettingsPort,
            deleteEncryptionSettingsPort,
            loadServerKeyPort,
            saveServerKeyPort,
            deleteServerKeyPort,
            serverKeyEncryptionPort,
            eventPublisher,
        )
    }

    describe("setup") {
        val memberId = MemberId.new()
        val salt = Base64.getEncoder().encodeToString("test-salt-16bytes".toByteArray())
        val encryptedDEK = Base64.getEncoder().encodeToString("test-encrypted-dek".toByteArray())
        val recoveryEncryptedDEK = Base64.getEncoder().encodeToString("test-recovery-encrypted-dek".toByteArray())
        val recoveryKeyHash = "hashed-recovery-key"
        val command = SetupEncryptionCommand(
            salt = salt,
            encryptedDEK = encryptedDEK,
            recoveryEncryptedDEK = recoveryEncryptedDEK,
            recoveryKeyHash = recoveryKeyHash,
        )
        val serverKeyResult = EncryptedServerKeyResult(
            encryptedServerKey = "encrypted-server-key",
            iv = "server-key-iv",
            plainServerKey = "plain-server-key",
        )

        context("암호화 설정이 없는 경우") {

            it("새 암호화 설정과 Server Key가 생성되고 저장된다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns false
                every { serverKeyEncryptionPort.generateAndEncryptServerKey() } returns serverKeyResult
                coEvery { saveServerKeyPort.save(any()) } answers { firstArg() }
                coEvery { saveEncryptionSettingsPort.save(any()) } answers { firstArg() }

                val result = encryptionSettingsService.setup(memberId, command)

                result shouldNotBe null
                result.settings.memberId shouldBe memberId
                result.settings.salt shouldBe salt
                result.settings.encryptedDEK shouldBe encryptedDEK
                result.settings.recoveryEncryptedDEK shouldBe recoveryEncryptedDEK
                result.settings.recoveryKeyHash shouldBe recoveryKeyHash
                result.serverKey shouldBe serverKeyResult.plainServerKey

                coVerify(exactly = 1) { loadEncryptionSettingsPort.existsByMemberId(memberId) }
                coVerify(exactly = 1) { serverKeyEncryptionPort.generateAndEncryptServerKey() }
                coVerify(exactly = 1) { saveServerKeyPort.save(any()) }
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
                coVerify(exactly = 0) { saveServerKeyPort.save(any()) }
            }
        }
    }

    describe("getSettings") {
        val memberId = MemberId.new()
        val plainServerKey = "decrypted-server-key"

        context("암호화 설정이 존재하는 경우") {

            it("설정과 복호화된 Server Key를 반환한다") {
                val existingSettings = createEncryptionSettings(memberId)
                val existingServerKey = createServerKey(memberId)
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings
                coEvery { loadServerKeyPort.findByMemberId(memberId) } returns existingServerKey
                every {
                    serverKeyEncryptionPort.decryptServerKey(
                        existingServerKey.encryptedServerKey,
                        existingServerKey.iv
                    )
                } returns plainServerKey

                val result = encryptionSettingsService.getSettings(memberId)

                result.salt shouldBe existingSettings.salt
                result.encryptedDEK shouldBe existingSettings.encryptedDEK
                result.serverKey shouldBe plainServerKey
                coVerify(exactly = 1) { loadEncryptionSettingsPort.findByMemberId(memberId) }
                coVerify(exactly = 1) { loadServerKeyPort.findByMemberId(memberId) }
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

    describe("getRecoverySettings") {
        val memberId = MemberId.new()

        context("암호화 설정이 존재하는 경우") {

            it("복구용 설정을 반환한다") {
                val existingSettings = createEncryptionSettings(memberId)
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings

                val result = encryptionSettingsService.getRecoverySettings(memberId)

                result.recoveryEncryptedDEK shouldBe existingSettings.recoveryEncryptedDEK
                result.recoveryKeyHash shouldBe existingSettings.recoveryKeyHash
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.getRecoverySettings(memberId)
                }
            }
        }
    }

    describe("updateEncryption") {
        val memberId = MemberId.new()
        val newSalt = "new-salt-value"
        val newEncryptedDEK = "new-encrypted-dek-value"
        val command = UpdateEncryptionCommand(
            salt = newSalt,
            encryptedDEK = newEncryptedDEK,
        )
        val newServerKeyResult = EncryptedServerKeyResult(
            encryptedServerKey = "new-encrypted-server-key",
            iv = "new-server-key-iv",
            plainServerKey = "new-plain-server-key",
        )

        context("암호화 설정이 존재하는 경우") {

            it("암호화 키와 Server Key가 업데이트된다") {
                val existingSettings = createEncryptionSettings(memberId)
                val existingServerKey = createServerKey(memberId)
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings
                coEvery { loadServerKeyPort.findByMemberId(memberId) } returns existingServerKey
                every { serverKeyEncryptionPort.generateAndEncryptServerKey() } returns newServerKeyResult
                coEvery { saveServerKeyPort.save(any()) } answers { firstArg() }
                coEvery { saveEncryptionSettingsPort.save(any()) } answers { firstArg() }

                val result = encryptionSettingsService.updateEncryption(memberId, command)

                result.settings.salt shouldBe newSalt
                result.settings.encryptedDEK shouldBe newEncryptedDEK
                result.serverKey shouldBe newServerKeyResult.plainServerKey

                coVerify(exactly = 1) { saveEncryptionSettingsPort.save(any()) }
                coVerify(exactly = 1) { saveServerKeyPort.save(any()) }
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.updateEncryption(memberId, command)
                }
            }
        }
    }

    describe("updateRecoveryKey") {
        val memberId = MemberId.new()
        val newRecoveryEncryptedDEK = "new-recovery-encrypted-dek"
        val newRecoveryKeyHash = "new-recovery-key-hash"
        val command = UpdateRecoveryKeyCommand(
            recoveryEncryptedDEK = newRecoveryEncryptedDEK,
            recoveryKeyHash = newRecoveryKeyHash,
        )

        context("암호화 설정이 존재하는 경우") {

            it("복구 키가 업데이트된다") {
                val existingSettings = createEncryptionSettings(memberId)
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns existingSettings
                coEvery { saveEncryptionSettingsPort.save(any()) } answers { firstArg() }

                val result = encryptionSettingsService.updateRecoveryKey(memberId, command)

                result.recoveryEncryptedDEK shouldBe newRecoveryEncryptedDEK
                result.recoveryKeyHash shouldBe newRecoveryKeyHash

                coVerify(exactly = 1) { saveEncryptionSettingsPort.save(any()) }
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.findByMemberId(memberId) } returns null

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.updateRecoveryKey(memberId, command)
                }
            }
        }
    }

    describe("deleteSettings") {
        val memberId = MemberId.new()

        context("암호화 설정이 존재하는 경우") {

            it("암호화 설정과 Server Key를 삭제하고 Integration Event를 발행한다") {
                val eventSlot = slot<Any>()
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns true
                coEvery { deleteEncryptionSettingsPort.deleteByMemberId(memberId) } just Runs
                coEvery { deleteServerKeyPort.deleteByMemberId(memberId) } just Runs
                every { eventPublisher.publishEvent(capture(eventSlot)) } just Runs

                encryptionSettingsService.deleteSettings(memberId)

                coVerify(exactly = 1) { loadEncryptionSettingsPort.existsByMemberId(memberId) }
                coVerify(exactly = 1) { deleteEncryptionSettingsPort.deleteByMemberId(memberId) }
                coVerify(exactly = 1) { deleteServerKeyPort.deleteByMemberId(memberId) }

                // 발행된 Integration Event 검증
                eventSlot.isCaptured shouldBe true
                val capturedEvent = eventSlot.captured
                capturedEvent::class.simpleName shouldBe "EncryptionSettingsDeletedIntegrationEvent"
            }
        }

        context("암호화 설정이 없는 경우") {

            it("EncryptionSettingsNotFoundException을 던진다") {
                coEvery { loadEncryptionSettingsPort.existsByMemberId(memberId) } returns false

                shouldThrow<EncryptionSettingsNotFoundException> {
                    encryptionSettingsService.deleteSettings(memberId)
                }

                coVerify(exactly = 0) { deleteEncryptionSettingsPort.deleteByMemberId(memberId) }
                coVerify(exactly = 0) { deleteServerKeyPort.deleteByMemberId(memberId) }
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

private fun createServerKey(
    memberId: MemberId = MemberId.new(),
    encryptedServerKey: String = "test-encrypted-server-key",
    iv: String = "test-server-key-iv",
): ServerKey {
    return ServerKey.create(
        memberId = memberId,
        encryptedServerKey = encryptedServerKey,
        iv = iv,
    )
}

// endregion
