package io.clroot.selah.domains.member.adapter.outbound.persistence.apikey

import io.clroot.selah.common.util.HexSupport.hashSha256
import io.clroot.selah.common.util.HexSupport.toHexString
import io.clroot.selah.common.util.ULIDSupport
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
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
    private val repository: ApiKeyJpaRepository,
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
    ): ApiKeyCreateResult = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val id = ULIDSupport.generateULID()

        // 랜덤 키 생성
        val randomPart = generateRandomKey()
        val rawKey = "$apiKeyPrefix$randomPart"

        // 해시 생성
        val keyHash = hashKey(rawKey)

        // 접두사 저장 (표시용)
        val displayPrefix = "$apiKeyPrefix${randomPart.take(PREFIX_DISPLAY_LENGTH)}"

        val entity = ApiKeyEntity(
            id = id,
            keyHash = keyHash,
            keyPrefix = displayPrefix,
            memberId = memberId.value,
            role = role,
            name = name,
            createdIp = ipAddress?.take(45),
            createdAt = now,
        )

        repository.save(entity)

        ApiKeyCreateResult(
            info = entity.toApiKeyInfo(),
            rawKey = rawKey,
        )
    }

    override suspend fun findByKey(apiKey: String): ApiKeyInfo? = withContext(Dispatchers.IO) {
        val keyHash = hashKey(apiKey)
        repository.findByKeyHash(keyHash)?.toApiKeyInfo()
    }

    @Transactional
    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    override suspend fun findAllByMemberId(memberId: MemberId): List<ApiKeyInfo> =
        withContext(Dispatchers.IO) {
            repository.findAllByMemberId(memberId.value).map { it.toApiKeyInfo() }
        }

    @Transactional
    override suspend fun updateLastUsedAt(id: String, ipAddress: String?) {
        withContext(Dispatchers.IO) {
            val entity = repository.findByIdOrNull(id) ?: return@withContext
            entity.lastUsedAt = LocalDateTime.now()
            entity.lastUsedIp = ipAddress?.take(45)
            repository.save(entity)
        }
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

    private fun ApiKeyEntity.toApiKeyInfo(): ApiKeyInfo = ApiKeyInfo(
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
