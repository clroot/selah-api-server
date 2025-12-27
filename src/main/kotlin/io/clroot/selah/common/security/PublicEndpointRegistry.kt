package io.clroot.selah.common.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * @PublicEndpoint 어노테이션이 붙은 엔드포인트를 수집하는 레지스트리
 *
 * 애플리케이션 시작 시 모든 컨트롤러를 스캔하여
 * @PublicEndpoint가 붙은 메서드들의 경로를 수집합니다.
 */
@Component
class PublicEndpointRegistry(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 공개 엔드포인트 목록을 RequestMatcher로 반환
     */
    fun getPublicEndpointMatcher(): RequestMatcher {
        val matchers = mutableListOf<RequestMatcher>()

        requestMappingHandlerMapping.handlerMethods.forEach { (mappingInfo, handlerMethod) ->
            if (isPublicEndpoint(handlerMethod)) {
                val requestMatchers = createRequestMatchers(mappingInfo)
                matchers.addAll(requestMatchers)

                logPublicEndpoint(mappingInfo, handlerMethod)
            }
        }

        return if (matchers.isEmpty()) {
            // 공개 엔드포인트가 없으면 아무것도 매칭하지 않는 matcher 반환
            RequestMatcher { false }
        } else {
            OrRequestMatcher(matchers)
        }
    }

    private fun isPublicEndpoint(handlerMethod: HandlerMethod): Boolean {
        // 메서드에 @PublicEndpoint가 있는지 확인
        if (handlerMethod.hasMethodAnnotation(PublicEndpoint::class.java)) {
            return true
        }

        // 클래스에 @PublicEndpoint가 있는지 확인
        return handlerMethod.beanType.isAnnotationPresent(PublicEndpoint::class.java)
    }

    private fun createRequestMatchers(mappingInfo: RequestMappingInfo): List<RequestMatcher> {
        val matchers = mutableListOf<RequestMatcher>()

        val patterns = mappingInfo.patternValues
        val methods = mappingInfo.methodsCondition.methods

        patterns.forEach { pattern ->
            if (methods.isEmpty()) {
                // HTTP 메서드가 지정되지 않은 경우 모든 메서드에 대해 허용
                matchers.add(PathPatternRequestMatcher.withDefaults().matcher(pattern))
            } else {
                methods.forEach { method ->
                    val httpMethod = HttpMethod.valueOf(method.name)
                    matchers.add(PathPatternRequestMatcher.withDefaults().matcher(httpMethod, pattern))
                }
            }
        }

        return matchers
    }

    private fun logPublicEndpoint(
        mappingInfo: RequestMappingInfo,
        handlerMethod: HandlerMethod,
    ) {
        val methods = mappingInfo.methodsCondition.methods.ifEmpty { setOf("*") }
        val patterns = mappingInfo.patternValues

        logger.info(
            "Public endpoint registered: {} {} -> {}.{}",
            methods,
            patterns,
            handlerMethod.beanType.simpleName,
            handlerMethod.method.name,
        )
    }
}
