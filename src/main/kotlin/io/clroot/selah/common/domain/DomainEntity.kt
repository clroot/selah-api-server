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
 *     override val id: OAuthConnectionId? = null,
 *     val memberId: MemberId,
 *     val provider: AuthProvider,
 *     val providerId: String
 * ) : DomainEntity<OAuthConnectionId>()
 * ```
 *
 * @param ID 식별자 타입
 */
abstract class DomainEntity<ID : Any> {
    /**
     * Entity 식별자
     *
     * - ULID 기반 ID: 생성 시점에 할당
     * - Long 기반 ID: null이면 DB 저장 후 할당됨
     */
    abstract val id: ID?

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
