package io.clroot.selah.common.application

import io.clroot.selah.common.domain.AggregateRoot
import org.springframework.context.ApplicationEventPublisher

/**
 * Aggregate Root의 Domain Events를 발행하고 초기화하는 확장 함수
 *
 * Application Layer의 Service에서 Aggregate 저장 후 호출합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val savedPrayerTopic = savePrayerTopicPort.save(prayerTopic)
 * savedPrayerTopic.publishAndClearEvents(eventPublisher)
 * ```
 */
fun <ID : Any> AggregateRoot<ID>.publishAndClearEvents(eventPublisher: ApplicationEventPublisher) {
    domainEvents.forEach { event ->
        eventPublisher.publishEvent(event)
    }
    clearEvents()
}
