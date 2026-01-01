package io.clroot.selah.domains.member.domain

/**
 * 해시된 비밀번호를 래핑하는 Value Object
 *
 * Application Layer에서 해싱된 비밀번호를 도메인에 전달할 때 사용합니다.
 * Domain Layer는 해싱 알고리즘에 의존하지 않습니다.
 *
 * 보안을 위해 toString()은 "[PROTECTED]"를 반환합니다.
 */
@JvmInline
value class PasswordHash(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Password hash cannot be blank" }
    }

    override fun toString(): String = "[PROTECTED]"

    companion object {
        fun from(hashedPassword: String): PasswordHash = PasswordHash(hashedPassword)
    }
}
