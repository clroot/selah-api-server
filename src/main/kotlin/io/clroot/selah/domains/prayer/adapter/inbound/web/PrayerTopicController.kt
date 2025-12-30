package io.clroot.selah.domains.prayer.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.PageResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.CreatePrayerTopicRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.PrayerTopicResponse
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.UpdatePrayerTopicRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.toResponse
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.DeletePrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerTopicUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicTitleCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerTopicUseCase
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
    private val updatePrayerTopicUseCase: UpdatePrayerTopicUseCase,
    private val deletePrayerTopicUseCase: DeletePrayerTopicUseCase,
) {

    /**
     * 기도제목 생성
     */
    @PostMapping
    suspend fun create(
        @RequestBody request: CreatePrayerTopicRequest,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic = createPrayerTopicUseCase.create(
            CreatePrayerTopicCommand(
                memberId = memberId,
                title = request.title,
            ),
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(prayerTopic.toResponse()))
    }

    /**
     * 기도제목 목록 조회
     */
    @GetMapping
    suspend fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: PrayerTopicStatus?,
    ): ResponseEntity<ApiResponse<PageResponse<PrayerTopicResponse>>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = getPrayerTopicUseCase.listByMemberId(
            memberId = memberId,
            status = status,
            pageable = PageRequest.of(page, size),
        )

        return ResponseEntity.ok(
            ApiResponse.success(
                PageResponse(
                    content = result.content.map { it.toResponse() },
                    page = result.number,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                ),
            ),
        )
    }

    /**
     * 기도제목 단건 조회
     */
    @GetMapping("/{id}")
    suspend fun getById(
        @PathVariable id: String,
    ): ResponseEntity<ApiResponse<PrayerTopicResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayerTopic = getPrayerTopicUseCase.getById(
            id = PrayerTopicId.from(id),
            memberId = memberId,
        )
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse()))
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
        val prayerTopic = updatePrayerTopicUseCase.updateTitle(
            UpdatePrayerTopicTitleCommand(
                id = PrayerTopicId.from(id),
                memberId = memberId,
                title = request.title,
            ),
        )
        return ResponseEntity.ok(ApiResponse.success(prayerTopic.toResponse()))
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
}
