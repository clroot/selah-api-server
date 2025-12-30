package io.clroot.selah.domains.prayer.application.service

import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.inbound.AnswerPrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.CancelAnswerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.DeletePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.MarkAsAnsweredCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicTitleCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicUseCase
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기도제목 서비스
 */
@Service
@Transactional
class PrayerTopicService(
    private val savePrayerTopicPort: SavePrayerTopicPort,
    private val loadPrayerTopicPort: LoadPrayerTopicPort,
    private val deletePrayerTopicPort: DeletePrayerTopicPort,
    private val eventPublisher: ApplicationEventPublisher,
) : CreatePrayerTopicUseCase,
    GetPrayerTopicUseCase,
    UpdatePrayerTopicUseCase,
    DeletePrayerTopicUseCase,
    AnswerPrayerTopicUseCase {

    override suspend fun create(command: CreatePrayerTopicCommand): PrayerTopic {
        val prayerTopic = PrayerTopic.create(
            memberId = command.memberId,
            title = command.title,
        )

        val saved = savePrayerTopicPort.save(prayerTopic)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    @Transactional(readOnly = true)
    override suspend fun getById(id: PrayerTopicId, memberId: MemberId): PrayerTopic {
        val prayerTopic = loadPrayerTopicPort.findById(id)
            ?: throw PrayerTopicNotFoundException(id.value)

        if (prayerTopic.memberId != memberId) {
            throw PrayerTopicAccessDeniedException(id.value)
        }

        return prayerTopic
    }

    @Transactional(readOnly = true)
    override suspend fun listByMemberId(
        memberId: MemberId,
        status: PrayerTopicStatus?,
        pageable: Pageable,
    ): Page<PrayerTopic> {
        return loadPrayerTopicPort.findAllByMemberId(memberId, status, pageable)
    }

    override suspend fun updateTitle(command: UpdatePrayerTopicTitleCommand): PrayerTopic {
        val prayerTopic = loadPrayerTopicPort.findById(command.id)
            ?: throw PrayerTopicNotFoundException(command.id.value)

        if (prayerTopic.memberId != command.memberId) {
            throw PrayerTopicAccessDeniedException(command.id.value)
        }

        prayerTopic.updateTitle(command.title)

        val saved = savePrayerTopicPort.save(prayerTopic)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    override suspend fun delete(id: PrayerTopicId, memberId: MemberId) {
        val prayerTopic = loadPrayerTopicPort.findById(id)
            ?: throw PrayerTopicNotFoundException(id.value)

        if (prayerTopic.memberId != memberId) {
            throw PrayerTopicAccessDeniedException(id.value)
        }

        deletePrayerTopicPort.deleteById(id)
    }

    override suspend fun markAsAnswered(command: MarkAsAnsweredCommand): PrayerTopic {
        val prayerTopic = loadPrayerTopicPort.findById(command.id)
            ?: throw PrayerTopicNotFoundException(command.id.value)

        if (prayerTopic.memberId != command.memberId) {
            throw PrayerTopicAccessDeniedException(command.id.value)
        }

        if (prayerTopic.status == PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicAlreadyAnsweredException(command.id.value)
        }

        prayerTopic.markAsAnswered(command.reflection)

        val saved = savePrayerTopicPort.save(prayerTopic)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    override suspend fun cancelAnswer(command: CancelAnswerCommand): PrayerTopic {
        val prayerTopic = loadPrayerTopicPort.findById(command.id)
            ?: throw PrayerTopicNotFoundException(command.id.value)

        if (prayerTopic.memberId != command.memberId) {
            throw PrayerTopicAccessDeniedException(command.id.value)
        }

        if (prayerTopic.status != PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicNotAnsweredException(command.id.value)
        }

        prayerTopic.cancelAnswer()

        val saved = savePrayerTopicPort.save(prayerTopic)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    override suspend fun updateReflection(command: UpdateReflectionCommand): PrayerTopic {
        val prayerTopic = loadPrayerTopicPort.findById(command.id)
            ?: throw PrayerTopicNotFoundException(command.id.value)

        if (prayerTopic.memberId != command.memberId) {
            throw PrayerTopicAccessDeniedException(command.id.value)
        }

        if (prayerTopic.status != PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicNotAnsweredException(command.id.value)
        }

        prayerTopic.updateReflection(command.reflection)

        val saved = savePrayerTopicPort.save(prayerTopic)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }
}
