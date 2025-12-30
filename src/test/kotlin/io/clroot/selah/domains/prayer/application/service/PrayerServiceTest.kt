package io.clroot.selah.domains.prayer.application.service

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerContentCommand
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerPort
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.exception.PrayerAccessDeniedException
import io.clroot.selah.domains.prayer.domain.exception.PrayerNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class PrayerServiceTest : DescribeSpec({

    val savePrayerPort = mockk<SavePrayerPort>()
    val loadPrayerPort = mockk<LoadPrayerPort>()
    val deletePrayerPort = mockk<DeletePrayerPort>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val service = PrayerService(
        savePrayerPort = savePrayerPort,
        loadPrayerPort = loadPrayerPort,
        deletePrayerPort = deletePrayerPort,
        eventPublisher = eventPublisher,
    )

    beforeTest {
        clearMocks(savePrayerPort, loadPrayerPort, deletePrayerPort)
    }

    describe("기도문 생성") {

        it("새 기도문을 생성한다") {
            val memberId = MemberId.new()
            val content = "encrypted_content"
            val command = CreatePrayerCommand(memberId, content)

            val slot = slot<Prayer>()
            coEvery { savePrayerPort.save(capture(slot)) } answers { slot.captured }

            val result = service.create(command)

            result.memberId shouldBe memberId
            result.content shouldBe content

            coVerify(exactly = 1) { savePrayerPort.save(any()) }
        }
    }

    describe("기도문 조회") {

        context("getById") {

            it("소유자가 조회하면 기도문을 반환한다") {
                val memberId = MemberId.new()
                val prayer = createPrayer(memberId = memberId)

                coEvery { loadPrayerPort.findById(prayer.id) } returns prayer

                val result = service.getById(prayer.id, memberId)

                result shouldBe prayer
            }

            it("존재하지 않는 기도문을 조회하면 PrayerNotFoundException을 던진다") {
                val memberId = MemberId.new()
                val prayerId = PrayerId.new()

                coEvery { loadPrayerPort.findById(prayerId) } returns null

                shouldThrow<PrayerNotFoundException> {
                    service.getById(prayerId, memberId)
                }
            }

            it("다른 사용자의 기도문을 조회하면 PrayerAccessDeniedException을 던진다") {
                val ownerId = MemberId.new()
                val otherMemberId = MemberId.new()
                val prayer = createPrayer(memberId = ownerId)

                coEvery { loadPrayerPort.findById(prayer.id) } returns prayer

                shouldThrow<PrayerAccessDeniedException> {
                    service.getById(prayer.id, otherMemberId)
                }
            }
        }

        context("listByMemberId") {

            it("회원의 기도문 목록을 페이지네이션하여 반환한다") {
                val memberId = MemberId.new()
                val pageable = PageRequest.of(0, 10)
                val prayers = listOf(
                    createPrayer(memberId = memberId, content = "content1"),
                    createPrayer(memberId = memberId, content = "content2"),
                )
                val page = PageImpl(prayers, pageable, 2)

                coEvery { loadPrayerPort.findAllByMemberId(memberId, pageable) } returns page

                val result = service.listByMemberId(memberId, pageable)

                result.content.size shouldBe 2
                result.totalElements shouldBe 2
            }
        }
    }

    describe("기도문 수정") {

        context("updateContent") {

            it("소유자가 내용을 수정할 수 있다") {
                val memberId = MemberId.new()
                val prayer = createPrayer(memberId = memberId, content = "old_content")
                val command = UpdatePrayerContentCommand(
                    id = prayer.id,
                    memberId = memberId,
                    content = "new_content",
                )

                coEvery { loadPrayerPort.findById(prayer.id) } returns prayer
                coEvery { savePrayerPort.save(any()) } answers { firstArg() }

                val result = service.updateContent(command)

                result.content shouldBe "new_content"
                coVerify(exactly = 1) { savePrayerPort.save(any()) }
            }

            it("존재하지 않는 기도문을 수정하면 PrayerNotFoundException을 던진다") {
                val memberId = MemberId.new()
                val prayerId = PrayerId.new()
                val command = UpdatePrayerContentCommand(
                    id = prayerId,
                    memberId = memberId,
                    content = "new_content",
                )

                coEvery { loadPrayerPort.findById(prayerId) } returns null

                shouldThrow<PrayerNotFoundException> {
                    service.updateContent(command)
                }
            }

            it("다른 사용자의 기도문을 수정하면 PrayerAccessDeniedException을 던진다") {
                val ownerId = MemberId.new()
                val otherMemberId = MemberId.new()
                val prayer = createPrayer(memberId = ownerId)
                val command = UpdatePrayerContentCommand(
                    id = prayer.id,
                    memberId = otherMemberId,
                    content = "new_content",
                )

                coEvery { loadPrayerPort.findById(prayer.id) } returns prayer

                shouldThrow<PrayerAccessDeniedException> {
                    service.updateContent(command)
                }
            }
        }
    }

    describe("기도문 삭제") {

        it("소유자가 기도문을 삭제할 수 있다") {
            val memberId = MemberId.new()
            val prayer = createPrayer(memberId = memberId)

            coEvery { loadPrayerPort.findById(prayer.id) } returns prayer
            coJustRun { deletePrayerPort.deleteById(prayer.id) }

            service.delete(prayer.id, memberId)

            coVerify(exactly = 1) { deletePrayerPort.deleteById(prayer.id) }
        }

        it("존재하지 않는 기도문을 삭제하면 PrayerNotFoundException을 던진다") {
            val memberId = MemberId.new()
            val prayerId = PrayerId.new()

            coEvery { loadPrayerPort.findById(prayerId) } returns null

            shouldThrow<PrayerNotFoundException> {
                service.delete(prayerId, memberId)
            }
        }

        it("다른 사용자의 기도문을 삭제하면 PrayerAccessDeniedException을 던진다") {
            val ownerId = MemberId.new()
            val otherMemberId = MemberId.new()
            val prayer = createPrayer(memberId = ownerId)

            coEvery { loadPrayerPort.findById(prayer.id) } returns prayer

            shouldThrow<PrayerAccessDeniedException> {
                service.delete(prayer.id, otherMemberId)
            }
        }
    }
})

private fun createPrayer(
    memberId: MemberId = MemberId.new(),
    content: String = "encrypted_content",
): Prayer {
    val now = LocalDateTime.now()
    return Prayer(
        id = PrayerId.new(),
        memberId = memberId,
        content = content,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}
