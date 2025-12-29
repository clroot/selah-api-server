package io.clroot.selah.common.domain

/**
 * Aggregate 내부의 Entity를 위한 추상 클래스
 *
 * Aggregate Root를 통해서만 접근 가능하며, 독립적인 생명주기를 갖지 않습니다.
 * 도메인 이벤트는 Aggregate Root에서 관리합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class OAuthConnection(
 *     id: OAuthConnectionId?,
 *     // --- 비즈니스 필드 ---
 *     val memberId: MemberId,
 *     val provider: OAuthProvider,
 *     val providerId: String,
 *     val connectedAt: LocalDateTime,
 * ) : DomainEntity<OAuthConnectionId>(id)
 *
 * companion object {
 *     fun create(provider: OAuthProvider, providerId: String) = OAuthConnection(
 *         id = OAuthConnectionId.new(),
 *         memberId = memberId,
 *         provider = provider,
 *         providerId = providerId,
 *         connectedAt = LocalDateTime.now(),
 *     )
 * }
 * ```
 *
 * @param ID 식별자 타입
 * @param id Entity 식별자 (ULID 기반: 생성 시 할당, Long 기반: DB 저장 후 할당)
 */
abstract class DomainEntity<ID : Any>(
    open val id: ID?,
) {
    /**
     * Entity 동등성 - ID 기반 비교
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as DomainEntity<*>
        return id != null && id == other.id
    }

    /**
     * ID 기반 해시코드
     */
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
