package io.clroot.selah.domains.member.domain

@JvmInline
value class Email(val value: String) {
    init {
        require(isValidEmail(value)) { "Invalid email format: $value" }
    }

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )

        private fun isValidEmail(email: String): Boolean {
            return email.isNotBlank() && EMAIL_REGEX.matches(email)
        }

        fun from(value: String): Email = Email(value)
    }
}
