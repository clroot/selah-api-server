package io.clroot.selah.domains.member.adapter.inbound.web.dto

import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.OAuthProvider
import java.time.LocalDateTime

// === Request DTOs ===

/**
 * 프로필 업데이트 요청
 */
data class UpdateProfileRequest(
    val nickname: String?,
    val profileImageUrl: String?,
)

/**
 * API Key 생성 요청
 */
data class CreateApiKeyRequest(
    val name: String,
)

// === Response DTOs ===

/**
 * 회원 프로필 응답
 */
data class MemberProfileResponse(
    val id: String,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val connectedProviders: Set<OAuthProvider>,
    val createdAt: LocalDateTime,
)

/**
 * API Key 목록 응답
 */
data class ApiKeyResponse(
    val id: String,
    val name: String,
    val prefix: String,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?,
)

/**
 * API Key 생성 응답 (원본 키 포함)
 */
data class ApiKeyCreateResponse(
    val id: String,
    val name: String,
    val prefix: String,
    val rawKey: String,
    val createdAt: LocalDateTime,
)

// === Extension Functions ===

fun Member.toProfileResponse(): MemberProfileResponse = MemberProfileResponse(
    id = id.value,
    email = email.value,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    emailVerified = emailVerified,
    hasPassword = hasPassword,
    connectedProviders = connectedProviders,
    createdAt = createdAt,
)

fun ApiKeyInfo.toResponse(): ApiKeyResponse = ApiKeyResponse(
    id = id,
    name = name,
    prefix = prefix,
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
)

fun ApiKeyCreateResult.toResponse(): ApiKeyCreateResponse = ApiKeyCreateResponse(
    id = info.id,
    name = info.name,
    prefix = info.prefix,
    rawKey = rawKey,
    createdAt = info.createdAt,
)
