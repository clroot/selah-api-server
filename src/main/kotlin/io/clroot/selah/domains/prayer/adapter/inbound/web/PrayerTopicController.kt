package io.clroot.selah.domains.prayer.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.PageResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.CreatePrayerTopicRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.MarkAsAnsweredRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.PrayerTopicResponse
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.UpdatePrayerTopicRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.UpdateReflectionRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.toResponse
import io.clroot.selah.domains.prayer.application.port.inbound.AnswerPrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.CancelAnswerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.DeletePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.MarkAsAnsweredCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicTitleCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.UpdateReflectionCommand
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 기도제목 API Controller
 */
@RestController
@RequestMapping("/api/v1/prayer-topics")
class PrayerTopicController(
    private val createPrayerTopicUseCase: CreatePrayerTopicUseCase,
    private val getPrayerTopicUseCase: GetPrayerTopicUseCase,
    private val getPrayerUseCase: GetPrayerUseCase,
    private val updatePrayerTopicUseCase: UpdatePrayerTopicUseCase,
    private val deletePrayerTopicUseCase: DeletePrayerTopicUseCase,
    private val answerPrayerTopicUseCase: AnswerPrayerTopicUseCase,
) {
    /**
     * 기도제목 생성
     */
    @PostMapping
    suspend fun create(
        @RequestBody request: CreatePrayerTopicRequest,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic =
            createPrayerTopicUseCase.create(
                CreatePrayerTopicCommand(
                    memberId = memberId,
                    title = request.title,
                ),
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopic.id))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopic.id] ?: 0)))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: PrayerTopicStatus?,
    ): ResponseEntity<ApiResponse<PageResponse<PrayerTopicResponse>>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result =
            getPrayerTopicUseCase.listByMemberId(
                memberId = memberId,
                status = status,
                pageable = PageRequest.of(page, size),
            )

        val prayerTopicIds = result.content.map { it.id }
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(prayerTopicIds)

        return ResponseEntity.ok(
            ApiResponse.success(
                PageResponse(
                    content = result.content.map { it.toResponse(prayerCounts[it.id] ?: 0) },
                    page = result.number,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                ),
            ),
        )
    }

    @GetMapping("/{id}")
    suspend fun getById(
        @PathVariable id: String,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopicId = PrayerTopicId.from(id)
        val prayerTopic =
            getPrayerTopicUseCase.getById(
                id = prayerTopicId,
                memberId = memberId,
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopicId))
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopicId] ?: 0)))
    }

    /**
     * 기도제목 수정 (title)
     */
    @PatchMapping("/{id}")
    suspend fun updateTitle(
        @PathVariable id: String,
        @RequestBody request: UpdatePrayerTopicRequest,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic =
            updatePrayerTopicUseCase.updateTitle(
                UpdatePrayerTopicTitleCommand(
                    id = PrayerTopicId.from(id),
                    memberId = memberId,
                    title = request.title,
                ),
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopic.id))
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopic.id] ?: 0)))
    }

    /**
     * 기도제목 삭제
     */
    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Unit> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        deletePrayerTopicUseCase.delete(
            id = PrayerTopicId.from(id),
            memberId = memberId,
        )
        return ResponseEntity.noContent().build()
    }

    /**
     * 응답 체크
     */
    @PostMapping("/{id}/answer")
    suspend fun markAsAnswered(
        @PathVariable id: String,
        @RequestBody(required = false) request: MarkAsAnsweredRequest?,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic =
            answerPrayerTopicUseCase.markAsAnswered(
                MarkAsAnsweredCommand(
                    id = PrayerTopicId.from(id),
                    memberId = memberId,
                    reflection = request?.reflection,
                ),
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopic.id))
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopic.id] ?: 0)))
    }

    /**
     * 응답 취소
     */
    @DeleteMapping("/{id}/answer")
    suspend fun cancelAnswer(
        @PathVariable id: String,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic =
            answerPrayerTopicUseCase.cancelAnswer(
                CancelAnswerCommand(
                    id = PrayerTopicId.from(id),
                    memberId = memberId,
                ),
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopic.id))
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopic.id] ?: 0)))
    }

    /**
     * 소감 수정
     */
    @PatchMapping("/{id}/reflection")
    suspend fun updateReflection(
        @PathVariable id: String,
        @RequestBody request: UpdateReflectionRequest,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic =
            answerPrayerTopicUseCase.updateReflection(
                UpdateReflectionCommand(
                    id = PrayerTopicId.from(id),
                    memberId = memberId,
                    reflection = request.reflection,
                ),
            )
        val prayerCounts = getPrayerUseCase.countByPrayerTopicIds(listOf(prayerTopic.id))
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse(prayerCounts[prayerTopic.id] ?: 0)))
    }
}
