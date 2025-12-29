package io.clroot.selah.common.domain

import io.clroot.selah.common.event.DomainEvent
import java.time.LocalDateTime

/**
 * Aggregate Root 추상 클래스 - 공통 메타 필드, 이벤트 저장 및 Entity 동등성 제공
 *
 * 모든 Aggregate Root가 공통으로 가지는 메타 필드(id, version, createdAt, updatedAt)를
 * 생성자로 전달받아 관리합니다. Domain Event도 내부적으로 관리하며, ID 기반 동등성을 보장합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class PrayerTopic(
 *     id: PrayerTopicId?,
 *     // --- 비즈니스 필드 ---
 *     val memberId: MemberId,
 *     title: String,
 *     status: PrayerStatus,
 *     // --- 메타 필드 (하단) ---
 *     version: Long?,
 *     createdAt: LocalDateTime,
 *     updatedAt: LocalDateTime,
 * ) : AggregateRoot<PrayerTopicId>(id, version, createdAt, updatedAt) {
 *
 *     var title: String = title
 *         private set
 *
 *     fun updateTitle(newTitle: String) {
 *         if (title != newTitle) {
 *             title = newTitle
 *             touch()
 *         }
 *     }
 *
 *     companion object {
 *         fun create(memberId: MemberId, title: String): PrayerTopic {
 *             val now = LocalDateTime.now()
 *             return PrayerTopic(
 *                 id = PrayerTopicId.new(),
 *                 memberId = memberId,
 *                 title = title,
 *                 status = PrayerStatus.PRAYING,
 *                 version = null,
 *                 createdAt = now,
 *                 updatedAt = now,
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @param ID 식별자 타입 (MemberId, PrayerTopicId 등)
 * @param id Entity 식별자 (ULID 기반: 생성 시 할당, Long 기반: DB 저장 후 할당)
 * @param version 낙관적 락 버전 (JPA @Version과 매핑, 새 객체는 null)
 * @param createdAt 생성 시점 (불변)
 * @param updatedAt 수정 시점 (비즈니스 메서드에서 touch()로 갱신)
 */
abstract class AggregateRoot<ID : Any>(
    open val id: ID?,
    open val version: Long?,
    open val createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) {
    /**
     * 수정 시점
     *
     * 비즈니스 메서드에서 상태 변경 시 touch()를 호출하여 갱신합니다.
     * protected set으로 자식 클래스에서만 변경 가능합니다.
     */
    open var updatedAt: LocalDateTime = updatedAt
        protected set

    /**
     * updatedAt을 현재 시간으로 갱신
     *
     * 비즈니스 메서드에서 상태 변경 후 호출합니다.
     */
    protected fun touch() {
        updatedAt = LocalDateTime.now()
    }

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
