package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

/**
 * API Key Persistence Adapter (DB 기반)
 *
 * ApiKeyPort를 구현합니다.
 */
@Component
class ApiKeyPersistenceAdapter(
    private val sessionFactory: Mutiny.SessionFactory,
    private val jpqlRenderContext: JpqlRenderContext,
    @Value($$"${selah.api-key.prefix:selah_}")
    private val apiKeyPrefix: String,
) : ApiKeyPort {
    companion object {
        private const val KEY_LENGTH = 32
        private const val PREFIX_DISPLAY_LENGTH = 8
        private val SECURE_RANDOM = SecureRandom()
    }

    override suspend fun create(
        memberId: MemberId,
        role: Member.Role,
        name: String,
        ipAddress: String?,
    ): ApiKeyCreateResult =
        sessionFactory
            .withTransaction { session ->
                val now = LocalDateTime.now()
                val id = ULIDSupport.generateULID()

                // 랜덤 키 생성
                val randomPart = generateRandomKey()
                val rawKey = "$apiKeyPrefix$randomPart"

                // 해시 생성
                val keyHash = hashKey(rawKey)

                // 접두사 저장 (표시용)
                val displayPrefix = "$apiKeyPrefix${randomPart.take(PREFIX_DISPLAY_LENGTH)}"

                val entity =
                    ApiKeyEntity(
                        id = id,
                        keyHash = keyHash,
                        keyPrefix = displayPrefix,
                        memberId = memberId.value,
                        role = role,
                        name = name,
                        createdIp = ipAddress?.take(45),
                        createdAt = now,
                    )

                session.persist(entity).replaceWith(
                    ApiKeyCreateResult(
                        info = entity.toApiKeyInfo(),
                        rawKey = rawKey,
                    ),
                )
            }.awaitSuspending()

    override suspend fun findByKey(apiKey: String): ApiKeyInfo? =
        sessionFactory
            .withSession { session ->
                val keyHash = hashKey(apiKey)
                val query =
                    jpql {
                        select(entity(ApiKeyEntity::class))
                            .from(entity(ApiKeyEntity::class))
                            .where(path(ApiKeyEntity::keyHash).eq(keyHash))
                    }
                session.createQuery(query, jpqlRenderContext).singleResultOrNull
            }.awaitSuspending()
            ?.toApiKeyInfo()

    @Transactional
    override suspend fun delete(id: String) {
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        deleteFrom(entity(ApiKeyEntity::class))
                            .where(path(ApiKeyEntity::id).eq(id))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    override suspend fun findAllByMemberId(memberId: MemberId): List<ApiKeyInfo> =
        sessionFactory
            .withSession { session ->
                val query =
                    jpql {
                        select(entity(ApiKeyEntity::class))
                            .from(entity(ApiKeyEntity::class))
                            .where(path(ApiKeyEntity::memberId).eq(memberId.value))
                    }
                session.createQuery(query, jpqlRenderContext).resultList
            }.awaitSuspending()
            .map { it.toApiKeyInfo() }

    @Transactional
    override suspend fun updateLastUsedAt(
        id: String,
        ipAddress: String?,
    ) {
        sessionFactory
            .withTransaction { session ->
                val query =
                    jpql {
                        update(entity(ApiKeyEntity::class))
                            .set(path(ApiKeyEntity::lastUsedAt), LocalDateTime.now())
                            .set(path(ApiKeyEntity::lastUsedIp), ipAddress?.take(45))
                            .where(path(ApiKeyEntity::id).eq(id))
                    }
                session.createMutationQuery(query, jpqlRenderContext).executeUpdate()
            }.awaitSuspending()
    }

    /**
     * 랜덤 키 생성 (hex 문자열)
     */
    private fun generateRandomKey(): String {
        val bytes = ByteArray(KEY_LENGTH)
        SECURE_RANDOM.nextBytes(bytes)
        return bytes.toHexString()
    }

    /**
     * 키 해싱 (SHA-256)
     */
    private fun hashKey(key: String): String = hashSha256(key)

    private fun ApiKeyEntity.toApiKeyInfo(): ApiKeyInfo =
        ApiKeyInfo(
            id = id,
            memberId = MemberId.from(memberId),
            role = role,
            name = name,
            prefix = keyPrefix,
            createdIp = createdIp,
            lastUsedIp = lastUsedIp,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
        )
}
