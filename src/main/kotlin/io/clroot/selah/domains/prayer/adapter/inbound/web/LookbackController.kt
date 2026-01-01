package io.clroot.selah.domains.prayer.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.LookbackResponse
import io.clroot.selah.domains.prayer.adapter.inbound.web.dto.toResponse
import io.clroot.selah.domains.prayer.application.port.inbound.GetLookbackUseCase
import io.clroot.selah.domains.prayer.application.port.inbound.RefreshLookbackUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/prayer-topics/lookback")
class LookbackController(
    private val getLookbackUseCase: GetLookbackUseCase,
    private val refreshLookbackUseCase: RefreshLookbackUseCase,
) {
    @GetMapping("/today")
    suspend fun getTodayLookback(): ResponseEntity<ApiResponse<LookbackResponse?>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = getLookbackUseCase.getTodayLookback(memberId)
        return ResponseEntity.ok(ApiResponse.success(result?.toResponse()))
    }

    @PostMapping("/refresh")
    suspend fun refresh(): ResponseEntity<ApiResponse<LookbackResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = refreshLookbackUseCase.refresh(memberId)
        return ResponseEntity.ok(ApiResponse.success(result.toResponse()))
    }
}
