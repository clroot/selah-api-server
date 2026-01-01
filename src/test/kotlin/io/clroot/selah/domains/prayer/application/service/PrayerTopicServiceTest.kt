package io.clroot.selah.domains.prayer.application.service

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.inbound.CancelAnswerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicCommand
import io.clroot.selah.domains.prayer.application.port.inbound.MarkAsAnsweredCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicTitleCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdateReflectionCommand
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerTopicPort
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicAccessDeniedException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicAlreadyAnsweredException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicNotAnsweredException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicNotFoundException
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

class PrayerTopicServiceTest :
    DescribeSpec({

        val savePrayerTopicPort = mockk<SavePrayerTopicPort>()
        val loadPrayerTopicPort = mockk<LoadPrayerTopicPort>()
        val deletePrayerTopicPort = mockk<DeletePrayerTopicPort>()
        val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

        val service =
            PrayerTopicService(
                savePrayerTopicPort = savePrayerTopicPort,
                loadPrayerTopicPort = loadPrayerTopicPort,
                deletePrayerTopicPort = deletePrayerTopicPort,
                eventPublisher = eventPublisher,
            )

        beforeTest {
            clearMocks(savePrayerTopicPort, loadPrayerTopicPort, deletePrayerTopicPort)
        }

        describe("기도제목 생성") {

            it("새 기도제목을 생성한다") {
                val memberId = MemberId.new()
                val title = "encrypted_title"
                val command = CreatePrayerTopicCommand(memberId, title)

                val slot = slot<PrayerTopic>()
                coEvery { savePrayerTopicPort.save(capture(slot)) } answers { slot.captured }

                val result = service.create(command)

                result.memberId shouldBe memberId
                result.title shouldBe title
                result.status shouldBe PrayerTopicStatus.PRAYING

                coVerify(exactly = 1) { savePrayerTopicPort.save(any()) }
            }
        }

        describe("기도제목 조회") {

            context("getById") {

                it("소유자가 조회하면 기도제목을 반환한다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId)

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    val result = service.getById(prayerTopic.id, memberId)

                    result shouldBe prayerTopic
                }

                it("존재하지 않는 기도제목을 조회하면 PrayerTopicNotFoundException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopicId = PrayerTopicId.new()

                    coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                    shouldThrow<PrayerTopicNotFoundException> {
                        service.getById(prayerTopicId, memberId)
                    }
                }

                it("다른 사용자의 기도제목을 조회하면 PrayerTopicAccessDeniedException을 던진다") {
                    val ownerId = MemberId.new()
                    val otherMemberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = ownerId)

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAccessDeniedException> {
                        service.getById(prayerTopic.id, otherMemberId)
                    }
                }
            }

            context("listByMemberId") {

                it("회원의 기도제목 목록을 페이지네이션하여 반환한다") {
                    val memberId = MemberId.new()
                    val pageable = PageRequest.of(0, 10)
                    val prayerTopics =
                        listOf(
                            createPrayerTopic(memberId = memberId, title = "title1"),
                            createPrayerTopic(memberId = memberId, title = "title2"),
                        )
                    val page = PageImpl(prayerTopics, pageable, 2)

                    coEvery { loadPrayerTopicPort.findAllByMemberId(memberId, null, pageable) } returns page

                    val result = service.listByMemberId(memberId, null, pageable)

                    result.content.size shouldBe 2
                    result.totalElements shouldBe 2
                }

                it("상태로 필터링할 수 있다") {
                    val memberId = MemberId.new()
                    val pageable = PageRequest.of(0, 10)
                    val prayerTopics =
                        listOf(
                            createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.PRAYING),
                        )
                    val page = PageImpl(prayerTopics, pageable, 1)

                    coEvery {
                        loadPrayerTopicPort.findAllByMemberId(memberId, PrayerTopicStatus.PRAYING, pageable)
                    } returns page

                    val result = service.listByMemberId(memberId, PrayerTopicStatus.PRAYING, pageable)

                    result.content.size shouldBe 1
                    result.content.first().status shouldBe PrayerTopicStatus.PRAYING
                }
            }
        }

        describe("기도제목 수정") {

            context("updateTitle") {

                it("소유자가 제목을 수정할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, title = "old_title")
                    val command =
                        UpdatePrayerTopicTitleCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            title = "new_title",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.updateTitle(command)

                    result.title shouldBe "new_title"
                    coVerify(exactly = 1) { savePrayerTopicPort.save(any()) }
                }

                it("존재하지 않는 기도제목을 수정하면 PrayerTopicNotFoundException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopicId = PrayerTopicId.new()
                    val command =
                        UpdatePrayerTopicTitleCommand(
                            id = prayerTopicId,
                            memberId = memberId,
                            title = "new_title",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                    shouldThrow<PrayerTopicNotFoundException> {
                        service.updateTitle(command)
                    }
                }

                it("다른 사용자의 기도제목을 수정하면 PrayerTopicAccessDeniedException을 던진다") {
                    val ownerId = MemberId.new()
                    val otherMemberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = ownerId)
                    val command =
                        UpdatePrayerTopicTitleCommand(
                            id = prayerTopic.id,
                            memberId = otherMemberId,
                            title = "new_title",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAccessDeniedException> {
                        service.updateTitle(command)
                    }
                }
            }
        }

        describe("기도제목 삭제") {

            it("소유자가 기도제목을 삭제할 수 있다") {
                val memberId = MemberId.new()
                val prayerTopic = createPrayerTopic(memberId = memberId)

                coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                coJustRun { deletePrayerTopicPort.deleteById(prayerTopic.id) }

                service.delete(prayerTopic.id, memberId)

                coVerify(exactly = 1) { deletePrayerTopicPort.deleteById(prayerTopic.id) }
            }

            it("존재하지 않는 기도제목을 삭제하면 PrayerTopicNotFoundException을 던진다") {
                val memberId = MemberId.new()
                val prayerTopicId = PrayerTopicId.new()

                coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                shouldThrow<PrayerTopicNotFoundException> {
                    service.delete(prayerTopicId, memberId)
                }
            }

            it("다른 사용자의 기도제목을 삭제하면 PrayerTopicAccessDeniedException을 던진다") {
                val ownerId = MemberId.new()
                val otherMemberId = MemberId.new()
                val prayerTopic = createPrayerTopic(memberId = ownerId)

                coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                shouldThrow<PrayerTopicAccessDeniedException> {
                    service.delete(prayerTopic.id, otherMemberId)
                }
            }
        }

        describe("응답 체크") {

            context("markAsAnswered") {

                it("소유자가 기도제목을 응답 상태로 변경할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId)
                    val command =
                        MarkAsAnsweredCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = "encrypted_reflection",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.markAsAnswered(command)

                    result.status shouldBe PrayerTopicStatus.ANSWERED
                    result.reflection shouldBe "encrypted_reflection"
                    coVerify(exactly = 1) { savePrayerTopicPort.save(any()) }
                }

                it("소감 없이 응답할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId)
                    val command =
                        MarkAsAnsweredCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = null,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.markAsAnswered(command)

                    result.status shouldBe PrayerTopicStatus.ANSWERED
                    result.reflection shouldBe null
                }

                it("존재하지 않는 기도제목에 대해 호출하면 PrayerTopicNotFoundException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopicId = PrayerTopicId.new()
                    val command =
                        MarkAsAnsweredCommand(
                            id = prayerTopicId,
                            memberId = memberId,
                            reflection = null,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                    shouldThrow<PrayerTopicNotFoundException> {
                        service.markAsAnswered(command)
                    }
                }

                it("다른 사용자의 기도제목에 대해 호출하면 PrayerTopicAccessDeniedException을 던진다") {
                    val ownerId = MemberId.new()
                    val otherMemberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = ownerId)
                    val command =
                        MarkAsAnsweredCommand(
                            id = prayerTopic.id,
                            memberId = otherMemberId,
                            reflection = null,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAccessDeniedException> {
                        service.markAsAnswered(command)
                    }
                }

                it("이미 응답된 기도제목에 대해 호출하면 PrayerTopicAlreadyAnsweredException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        MarkAsAnsweredCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = null,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAlreadyAnsweredException> {
                        service.markAsAnswered(command)
                    }
                }
            }

            context("cancelAnswer") {

                it("소유자가 응답 상태를 취소할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        CancelAnswerCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.cancelAnswer(command)

                    result.status shouldBe PrayerTopicStatus.PRAYING
                    result.answeredAt shouldBe null
                    result.reflection shouldBe null
                    coVerify(exactly = 1) { savePrayerTopicPort.save(any()) }
                }

                it("존재하지 않는 기도제목에 대해 호출하면 PrayerTopicNotFoundException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopicId = PrayerTopicId.new()
                    val command =
                        CancelAnswerCommand(
                            id = prayerTopicId,
                            memberId = memberId,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                    shouldThrow<PrayerTopicNotFoundException> {
                        service.cancelAnswer(command)
                    }
                }

                it("다른 사용자의 기도제목에 대해 호출하면 PrayerTopicAccessDeniedException을 던진다") {
                    val ownerId = MemberId.new()
                    val otherMemberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = ownerId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        CancelAnswerCommand(
                            id = prayerTopic.id,
                            memberId = otherMemberId,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAccessDeniedException> {
                        service.cancelAnswer(command)
                    }
                }

                it("PRAYING 상태의 기도제목에 대해 호출하면 PrayerTopicNotAnsweredException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.PRAYING)
                    val command =
                        CancelAnswerCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicNotAnsweredException> {
                        service.cancelAnswer(command)
                    }
                }
            }

            context("updateReflection") {

                it("소유자가 응답된 기도제목의 소감을 수정할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        UpdateReflectionCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = "new_reflection",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.updateReflection(command)

                    result.reflection shouldBe "new_reflection"
                    coVerify(exactly = 1) { savePrayerTopicPort.save(any()) }
                }

                it("소감을 null로 설정하여 삭제할 수 있다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        UpdateReflectionCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = null,
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic
                    coEvery { savePrayerTopicPort.save(any()) } answers { firstArg() }

                    val result = service.updateReflection(command)

                    result.reflection shouldBe null
                }

                it("존재하지 않는 기도제목에 대해 호출하면 PrayerTopicNotFoundException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopicId = PrayerTopicId.new()
                    val command =
                        UpdateReflectionCommand(
                            id = prayerTopicId,
                            memberId = memberId,
                            reflection = "new_reflection",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopicId) } returns null

                    shouldThrow<PrayerTopicNotFoundException> {
                        service.updateReflection(command)
                    }
                }

                it("다른 사용자의 기도제목에 대해 호출하면 PrayerTopicAccessDeniedException을 던진다") {
                    val ownerId = MemberId.new()
                    val otherMemberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = ownerId, status = PrayerTopicStatus.ANSWERED)
                    val command =
                        UpdateReflectionCommand(
                            id = prayerTopic.id,
                            memberId = otherMemberId,
                            reflection = "new_reflection",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicAccessDeniedException> {
                        service.updateReflection(command)
                    }
                }

                it("PRAYING 상태의 기도제목에 대해 호출하면 PrayerTopicNotAnsweredException을 던진다") {
                    val memberId = MemberId.new()
                    val prayerTopic = createPrayerTopic(memberId = memberId, status = PrayerTopicStatus.PRAYING)
                    val command =
                        UpdateReflectionCommand(
                            id = prayerTopic.id,
                            memberId = memberId,
                            reflection = "new_reflection",
                        )

                    coEvery { loadPrayerTopicPort.findById(prayerTopic.id) } returns prayerTopic

                    shouldThrow<PrayerTopicNotAnsweredException> {
                        service.updateReflection(command)
                    }
                }
            }
        }
    })

private fun createPrayerTopic(
    memberId: MemberId = MemberId.new(),
    title: String = "encrypted_title",
    status: PrayerTopicStatus = PrayerTopicStatus.PRAYING,
): PrayerTopic {
    val now = LocalDateTime.now()
    return PrayerTopic(
        id = PrayerTopicId.new(),
        memberId = memberId,
        title = title,
        status = status,
        answeredAt = null,
        reflection = null,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}
