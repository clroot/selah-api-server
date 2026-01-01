package io.clroot.selah.domains.prayer.application.service

import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.DeletePrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerContentCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerUseCase
import io.clroot.selah.domains.prayer.application.port.outbound.DeletePrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerPort
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.exception.PrayerAccessDeniedException
import io.clroot.selah.domains.prayer.domain.exception.PrayerNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기도문 서비스
 */
@Service
@Transactional
class PrayerService(
    private val savePrayerPort: SavePrayerPort,
    private val loadPrayerPort: LoadPrayerPort,
    private val deletePrayerPort: DeletePrayerPort,
    private val eventPublisher: ApplicationEventPublisher,
) : CreatePrayerUseCase,
    GetPrayerUseCase,
    UpdatePrayerUseCase,
    DeletePrayerUseCase {
    override suspend fun create(command: CreatePrayerCommand): Prayer {
        val prayer =
            Prayer.create(
                memberId = command.memberId,
                prayerTopicIds = command.prayerTopicIds,
                content = command.content,
            )

        val saved = savePrayerPort.save(prayer)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    @Transactional(readOnly = true)
    override suspend fun getById(
        id: PrayerId,
        memberId: MemberId,
    ): Prayer {
        val prayer =
            loadPrayerPort.findById(id)
                ?: throw PrayerNotFoundException(id.value)

        if (prayer.memberId != memberId) {
            throw PrayerAccessDeniedException(id.value)
        }

        return prayer
    }

    @Transactional(readOnly = true)
    override suspend fun listByMemberId(
        memberId: MemberId,
        pageable: Pageable,
    ): Page<Prayer> = loadPrayerPort.findAllByMemberId(memberId, pageable)

    @Transactional(readOnly = true)
    override suspend fun listByPrayerTopicId(
        memberId: MemberId,
        prayerTopicId: PrayerTopicId,
        pageable: Pageable,
    ): Page<Prayer> = loadPrayerPort.findAllByMemberIdAndPrayerTopicId(memberId, prayerTopicId, pageable)

    @Transactional(readOnly = true)
    override suspend fun countByPrayerTopicIds(prayerTopicIds: List<PrayerTopicId>): Map<PrayerTopicId, Long> =
        loadPrayerPort.countByPrayerTopicIds(prayerTopicIds)

    override suspend fun updateContent(command: UpdatePrayerContentCommand): Prayer {
        val prayer =
            loadPrayerPort.findById(command.id)
                ?: throw PrayerNotFoundException(command.id.value)

        if (prayer.memberId != command.memberId) {
            throw PrayerAccessDeniedException(command.id.value)
        }

        prayer.updateContent(command.content)

        val saved = savePrayerPort.save(prayer)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    override suspend fun update(command: UpdatePrayerCommand): Prayer {
        val prayer =
            loadPrayerPort.findById(command.id)
                ?: throw PrayerNotFoundException(command.id.value)

        if (prayer.memberId != command.memberId) {
            throw PrayerAccessDeniedException(command.id.value)
        }

        prayer.update(command.content, command.prayerTopicIds)

        val saved = savePrayerPort.save(prayer)
        saved.publishAndClearEvents(eventPublisher)

        return saved
    }

    override suspend fun delete(
        id: PrayerId,
        memberId: MemberId,
    ) {
        val prayer =
            loadPrayerPort.findById(id)
                ?: throw PrayerNotFoundException(id.value)

        if (prayer.memberId != memberId) {
            throw PrayerAccessDeniedException(id.value)
        }

        deletePrayerPort.deleteById(id)
    }
}
