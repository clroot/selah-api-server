package io.clroot.selah.common.util

import jakarta.servlet.http.HttpServletRequest

/**
 * HTTP 요청 관련 유틸리티
 */
object HttpRequestUtils {

    /**
     * 클라이언트 IP 주소를 추출합니다.
     *
     * 프록시/로드밸런서 뒤에 있는 경우 X-Forwarded-For 헤더를 우선 확인합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    fun extractIpAddress(request: HttpServletRequest): String? {
        // X-Forwarded-For 헤더 확인 (프록시/로드밸런서 뒤에 있는 경우)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            // 첫 번째 IP가 원본 클라이언트 IP
            return xForwardedFor.split(",").firstOrNull()?.trim()
        }
        return request.remoteAddr
    }
}
