package io.clroot.selah.common.event

import java.time.Instant
import java.util.UUID

/**
 * Domain Event - 같은 Bounded Context 내부에서 사용되는 이벤트
 *
 * Domain 객체의 상태 변경을 나타내며, Domain 객체를 직접 포함할 수 있습니다.
 * Application Layer에서 수신하여 처리하거나 Integration Event로 변환합니다.
 *
 * 위치: {context}/domain/event/
 */
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
}

/**
 * Domain Event 기본 구현을 위한 추상 클래스
 * data class에서 상속받아 사용합니다.
 */
abstract class BaseDomainEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
