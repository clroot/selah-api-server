package io.clroot.selah.domains.member.domain

/**
 * 평문 비밀번호를 래핑하는 sealed interface
 *
 * 두 가지 구현체를 제공합니다:
 * - [RawPassword]: 로그인 시도 시 사용 (검증 없음, 비밀번호 정책 노출 방지)
 * - [NewPassword]: 회원가입/비밀번호 변경 시 사용 (엄격한 검증 포함)
 */
sealed interface Password {
    val value: String
}

/**
 * 로그인 시도용 비밀번호
 *
 * 보안상의 이유로 비밀번호 규칙을 검증하지 않습니다.
 * (비밀번호 정책 노출 방지)
 */
@JvmInline
value class RawPassword(override val value: String) : Password {
    init {
        require(value.isNotEmpty()) { "Password cannot be empty" }
    }
}

/**
 * 회원가입/비밀번호 변경용 새 비밀번호
 *
 * 다음 규칙을 충족해야 합니다:
 * - 최소 8자 이상
 * - 영문자 1개 이상
 * - 숫자 1개 이상
 * - 특수문자 1개 이상
 */
@JvmInline
value class NewPassword private constructor(override val value: String) : Password {
    companion object {
        private val LETTER_REGEX = Regex("[a-zA-Z]")
        private val DIGIT_REGEX = Regex("[0-9]")
        private val SPECIAL_CHAR_REGEX = Regex("[!@#\$%^&*(),.?\":{}|<>]")

        fun from(plainText: String): NewPassword {
            require(plainText.length >= 8) { "Password must be at least 8 characters" }
            require(LETTER_REGEX.containsMatchIn(plainText)) { "Password must contain at least one letter" }
            require(DIGIT_REGEX.containsMatchIn(plainText)) { "Password must contain at least one digit" }
            require(SPECIAL_CHAR_REGEX.containsMatchIn(plainText)) { "Password must contain at least one special character" }
            return NewPassword(plainText)
        }
    }
}
