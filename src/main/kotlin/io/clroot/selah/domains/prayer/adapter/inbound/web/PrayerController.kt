package io.clroot.selah.domains.prayer.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.PageResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.CreatePrayerRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.PrayerResponse
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.UpdatePrayerRequest
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.toResponse
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerCommand
import io.clroot.selah.domains.prayer.application.port.inbound.CreatePrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.DeletePrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.GetPrayerUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerContentCommand
import io.clroot.selah.domains.prayer.application.port.inbound.UpdatePrayerUseCase
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
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
 * 기도문 API Controller
 */
@RestController
@RequestMapping("/api/v1/prayers")
class PrayerController(
    private val createPrayerUseCase: CreatePrayerUseCase,
    private val getPrayerUseCase: GetPrayerUseCase,
    private val updatePrayerUseCase: UpdatePrayerUseCase,
    private val deletePrayerUseCase: DeletePrayerUseCase,
) {

    /**
     * 기도문 생성
     */
    @PostMapping
    suspend fun create(
        @RequestBody request: CreatePrayerRequest,
    ): ResponseEntity<ApiResponse<PrayerResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayer = createPrayerUseCase.create(
            CreatePrayerCommand(
                memberId = memberId,
                prayerTopicIds = request.prayerTopicIds.map { PrayerTopicId.from(it) },
                content = request.content,
            ),
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(prayer.toResponse()))
    }

    /**
     * 기도문 목록 조회
     */
    @GetMapping
    suspend fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<PageResponse<PrayerResponse>>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = getPrayerUseCase.listByMemberId(
            memberId = memberId,
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
     * 기도문 단건 조회
     */
    @GetMapping("/{id}")
    suspend fun getById(
        @PathVariable id: String,
    ): ResponseEntity<ApiResponse<PrayerResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayer = getPrayerUseCase.getById(
            id = PrayerId.from(id),
            memberId = memberId,
        )
        return ResponseEntity.ok(ApiResponse.success(prayer.toResponse()))
    }

    /**
     * 기도문 수정 (content)
     */
    @PatchMapping("/{id}")
    suspend fun updateContent(
        @PathVariable id: String,
        @RequestBody request: UpdatePrayerRequest,
    ): ResponseEntity<ApiResponse<PrayerResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val prayer = updatePrayerUseCase.updateContent(
            UpdatePrayerContentCommand(
                id = PrayerId.from(id),
                memberId = memberId,
                content = request.content,
            ),
        )
        return ResponseEntity.ok(ApiResponse.success(prayer.toResponse()))
    }

    /**
     * 기도문 삭제
     */
    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Unit> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        deletePrayerUseCase.delete(
            id = PrayerId.from(id),
            memberId = memberId,
        )
        return ResponseEntity.noContent().build()
    }
}
