package io.clroot.selah.domains.member.domain

enum class OAuthProvider(
    val displayName: String,
) {
    GOOGLE("Google"),
    KAKAO("Kakao"),
    NAVER("Naver"),
    ;

    companion object {
        fun fromString(value: String): OAuthProvider =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown OAuth provider: $value")
    }
}
