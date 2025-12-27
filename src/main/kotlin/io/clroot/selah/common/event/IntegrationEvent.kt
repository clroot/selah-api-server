package io.clroot.selah.common.event

import java.time.Instant
import java.util.UUID

/**
 * Integration Event - Bounded Context 간 통신에 사용되는 이벤트
 *
 * Context 경계를 넘어 전달되므로 Domain 객체를 직접 포함하지 않고,
 * Snapshot DTO만 사용하여 느슨한 결합을 유지합니다.
 *
 * 위치: {context}/application/event/
 */
interface IntegrationEvent {
    val eventId: String
    val occurredAt: Instant
}

/**
 * Integration Event 기본 구현을 위한 추상 클래스
 * data class에서 상속받아 사용합니다.
 */
abstract class BaseIntegrationEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
) : IntegrationEvent
