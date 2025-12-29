package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import io.clroot.selah.domains.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * API Key JPA Entity
 *
 * API Key 정보를 저장합니다.
 * 보안을 위해 원본 키는 저장하지 않고 해시만 저장합니다.
 */
@Entity
@Table(
    name = "api_keys",
    indexes = [
        Index(name = "idx_api_keys_member_id", columnList = "member_id"),
        Index(name = "idx_api_keys_key_hash", columnList = "key_hash", unique = true),
    ],
)
class ApiKeyEntity(
    /**
     * API Key ID (ULID)
     */
    @Id
    @Column(name = "id", length = 26)
    val id: String,

    /**
     * API Key 해시 (원본 저장 안함)
     */
    @Column(name = "key_hash", nullable = false, unique = true)
    val keyHash: String,

    /**
     * API Key 접두사 (표시용, "selah_xxxxxxxx")
     */
    @Column(name = "key_prefix", nullable = false, length = 16)
    val keyPrefix: String,

    /**
     * 회원 ID
     */
    @Column(name = "member_id", nullable = false, length = 26)
    val memberId: String,

    /**
     * 회원 역할 (캐시)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Member.Role,

    /**
     * API Key 이름 (구분용)
     */
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    /**
     * 생성 시 IP 주소
     */
    @Column(name = "created_ip", length = 45)
    val createdIp: String?,

    /**
     * 마지막 사용 IP 주소
     */
    @Column(name = "last_used_ip", length = 45)
    var lastUsedIp: String? = null,

    /**
     * 마지막 사용 시간
     */
    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime? = null,

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
)
