package io.clroot.selah.common.domain

/**
 * Aggregate Root 식별자를 위한 공통 인터페이스
 *
 * 각 Bounded Context의 Aggregate Root는 고유한 ID 타입을 가집니다.
 * 제네릭 타입 T를 통해 다양한 ID 타입을 지원합니다:
 * - String: ULID 기반 ID (MemberId 등)
 * - Long: DB Auto-increment ID
 * - 기타 Value Object: ISBN 등
 *
 * 사용 예시:
 * ```kotlin
 * // ULID 기반 ID
 * @JvmInline
 * value class MemberId(override val value: String) : AggregateId<String> {
 *     init { require(ULIDSupport.isValidULID(value)) { "Invalid ULID format" } }
 *     companion object {
 *         fun new(): MemberId = MemberId(ULIDSupport.generateULID())
 *     }
 * }
 *
 * // Long 기반 ID
 * @JvmInline
 * value class BookAnalysisId(override val value: Long) : AggregateId<Long>
 *
 * // 도메인 식별자 (ISBN)
 * @JvmInline
 * value class Isbn(override val value: String) : AggregateId<String> {
 *     init { require(value.matches(ISBN_REGEX)) }
 * }
 * ```
 *
 * @param T ID의 실제 값 타입 (String, Long 등)
 */
interface AggregateId<T> {
    val value: T
}
