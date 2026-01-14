package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createMutationQuery
import com.linecorp.kotlinjdsl.support.hibernate.reactive.extension.createQuery
import io.clroot.hibernate.reactive.ReactiveSessionProvider
import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.LocalDateTime

/**
 * ApiKey Persistence Adapter
 *
 * 단순 조회는 CoroutineCrudRepository를 사용하고,
 * 복잡한 쿼리(updateLastUsedAt 등)는 JDSL을 사용합니다.
 */
@Component
class ApiKeyPersistenceAdapter(
    private val repository: ApiKeyEntityRepository,
    private val sessions: ReactiveSessionProvider,
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
        sessions.write { session ->
            val now = LocalDateTime.now()
            val id = ULIDSupport.generateULID()
            val randomPart = generateRandomKey()
            val rawKey = "$apiKeyPrefix$randomPart"
            val keyHash = hashKey(rawKey)
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
        }

    override suspend fun findByKey(apiKey: String): ApiKeyInfo? {
        val keyHash = hashKey(apiKey)
        return repository.findByKeyHash(keyHash)?.toApiKeyInfo()
    }

    override suspend fun delete(id: String) {
        repository.deleteById(id)
    }

    override suspend fun findAllByMemberId(memberId: MemberId): List<ApiKeyInfo> =
        repository.findAllByMemberId(memberId.value).map { it.toApiKeyInfo() }

    override suspend fun updateLastUsedAt(
        id: String,
        ipAddress: String?,
    ) {
        sessions.write { session ->
            session
                .createMutationQuery(
                    jpql {
                        update(entity(ApiKeyEntity::class))
                            .set(path(ApiKeyEntity::lastUsedAt), LocalDateTime.now())
                            .set(path(ApiKeyEntity::lastUsedIp), ipAddress?.take(45))
                            .where(path(ApiKeyEntity::id).eq(id))
                    },
                    jpqlRenderContext,
                ).executeUpdate()
        }
    }

    private fun generateRandomKey(): String {
        val bytes = ByteArray(KEY_LENGTH)
        SECURE_RANDOM.nextBytes(bytes)
        return bytes.toHexString()
    }

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
