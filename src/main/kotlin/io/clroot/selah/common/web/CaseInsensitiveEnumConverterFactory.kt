package io.clroot.selah.common.web

import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory

/**
 * 대소문자를 무시하는 Enum Converter Factory
 *
 * URL path variable이나 query parameter에서 enum 값을 대소문자 구분 없이 변환합니다.
 * 예: /api/v1/members/me/oauth-connections/google → OAuthProvider.GOOGLE
 */
class CaseInsensitiveEnumConverterFactory : ConverterFactory<String, Enum<*>> {
    override fun <T : Enum<*>> getConverter(targetType: Class<T>): Converter<String, T> = CaseInsensitiveEnumConverter(targetType)

    private class CaseInsensitiveEnumConverter<T : Enum<*>>(
        private val enumType: Class<T>,
    ) : Converter<String, T> {
        override fun convert(source: String): T? {
            if (source.isBlank()) return null

            return enumType.enumConstants.firstOrNull { constant ->
                constant.name.equals(source, ignoreCase = true)
            } ?: throw IllegalArgumentException(
                "No enum constant ${enumType.canonicalName}.$source (case-insensitive)",
            )
        }
    }
}
