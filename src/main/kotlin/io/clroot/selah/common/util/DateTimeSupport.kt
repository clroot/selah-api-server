package io.clroot.selah.common.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 날짜/시간 관련 유틸리티.
 *
 * PostgreSQL timestamp는 마이크로초(μs) 정밀도를 지원하지만,
 * Java의 LocalDateTime은 나노초(ns) 정밀도를 가집니다.
 * DB 저장 후 조회 시 정밀도 차이로 인한 불일치를 방지하기 위해
 * 이 유틸리티를 사용하세요.
 *
 * @see [PostgreSQL Date/Time Types](https://www.postgresql.org/docs/current/datatype-datetime.html)
 */
object DateTimeSupport {
    /**
     * PostgreSQL timestamp 정밀도(마이크로초)에 맞춘 현재 시간 반환.
     *
     * `LocalDateTime.now()` 대신 이 메서드를 사용하면
     * DB 저장/조회 시 정밀도 불일치 문제를 방지할 수 있습니다.
     *
     * 사용 예:
     * ```kotlin
     * val now = DateTimeSupport.now()
     * val entity = Entity(createdAt = now, updatedAt = now)
     * ```
     */
    fun now(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

    /**
     * 주어진 LocalDateTime을 마이크로초 정밀도로 truncate.
     */
    fun truncateToMicros(dateTime: LocalDateTime): LocalDateTime =
        dateTime.truncatedTo(ChronoUnit.MICROS)
}
