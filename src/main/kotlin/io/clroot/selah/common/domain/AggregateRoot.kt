package io.clroot.selah.common.domain

import io.clroot.selah.common.event.DomainEvent

/**
 * Aggregate Root 추상 클래스 - 이벤트 저장 및 Entity 동등성 제공
 *
 * Domain Event를 내부적으로 관리하며, ID 기반 동등성을 보장합니다.
 * 이벤트는 equals/hashCode에서 제외되어 순수하게 ID로만 비교합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class Book(
 *     override val id: BookId? = null,
 *     val isbn: Isbn,
 *     val title: String
 * ) : AggregateRoot<BookId>() {
 *
 *     fun addReviews(reviews: List<Review>) {
 *         registerEvent(ReviewCollectedEvent(this, reviews))
 *     }
 * }
 * ```
 *
 * @param ID 식별자 타입 (MemberId, BookId, BookAnalysisId 등)
 */
abstract class AggregateRoot<ID : Any> {
    /**
     * Entity 식별자
     *
     * - ULID 기반 ID: 생성 시점에 할당 (DB 저장 전에도 사용 가능)
     * - Long 기반 ID: null이면 DB 저장 후 할당됨 (Auto-increment)
     */
    abstract val id: ID?

    /**
     * 도메인 이벤트 저장소 (equals/hashCode에서 제외)
     */
    @Transient
    private val _domainEvents: MutableList<DomainEvent> = mutableListOf()

    /**
     * 발생한 도메인 이벤트 목록 (읽기 전용)
     */
    val domainEvents: List<DomainEvent>
        get() = _domainEvents.toList()

    /**
     * 도메인 이벤트 등록
     * 비즈니스 메서드에서 호출하여 이벤트를 저장합니다.
     */
    protected fun registerEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }

    /**
     * 모든 이벤트 제거
     * Application Layer에서 이벤트 발행 후 호출합니다.
     */
    fun clearEvents() {
        _domainEvents.clear()
    }

    /**
     * 이벤트 존재 여부 확인
     */
    fun hasEvents(): Boolean = _domainEvents.isNotEmpty()

    /**
     * Entity 동등성 - ID 기반 비교
     * 같은 타입이고 ID가 같으면 동일한 Entity로 판단합니다.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AggregateRoot<*>
        return id != null && id == other.id
    }

    /**
     * ID 기반 해시코드
     */
    override fun hashCode(): Int = id?.hashCode() ?: 0
}
