package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.event.*
import io.clroot.selah.domains.member.domain.exception.CannotDisconnectLastLoginMethodException
import io.clroot.selah.domains.member.domain.exception.OAuthProviderAlreadyConnectedException
import io.clroot.selah.domains.member.domain.exception.OAuthProviderNotConnectedException
import io.clroot.selah.domains.member.domain.exception.PasswordNotSetException
import java.time.LocalDateTime

/**
 * Member Aggregate Root
 *
 * 회원 정보를 관리하는 Aggregate Root입니다.
 * 이메일/비밀번호 인증 또는 OAuth 인증을 지원합니다.
 * 프로필 관리 등을 담당합니다.
 * 한 회원이 여러 OAuth Provider를 연결할 수 있습니다.
 */
class Member(
    override val id: MemberId,
    // --- 비즈니스 필드 ---
    email: Email,
    nickname: String,
    profileImageUrl: String?,
    passwordHash: PasswordHash?,
    emailVerified: Boolean,
    oauthConnections: List<OAuthConnection>,
    role: Role,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<MemberId>(id, version, createdAt, updatedAt) {
    init {
        require(nickname.isNotBlank()) { "Nickname cannot be blank" }
        require(passwordHash != null || oauthConnections.isNotEmpty()) {
            "Member must have either a password or at least one OAuth connection"
        }
    }

    // region Enums
    enum class Role {
        USER,
        ADMIN,
    }
    // endregion

    // region Mutable state (private set)
    var email: Email = email
        private set

    var nickname: String = nickname
        private set

    var profileImageUrl: String? = profileImageUrl
        private set

    var passwordHash: PasswordHash? = passwordHash
        private set

    var emailVerified: Boolean = emailVerified
        private set

    var role: Role = role
        private set

    // OAuth 연결 목록 (타입 변환 필요: MutableList → List)
    private val _oauthConnections: MutableList<OAuthConnection> = oauthConnections.toMutableList()
    val oauthConnections: List<OAuthConnection> get() = _oauthConnections.toList()
    // endregion

    // region Computed properties

    /**
     * 비밀번호 기반 인증 사용 여부
     */
    val hasPassword: Boolean
        get() = passwordHash != null

    /**
     * OAuth 기반 인증 사용 여부
     */
    val hasOAuthConnection: Boolean
        get() = _oauthConnections.isNotEmpty()

    /**
     * 최초 가입한 OAuth Provider
     */
    val primaryProvider: OAuthProvider?
        get() = _oauthConnections.minByOrNull { it.connectedAt }?.provider

    /**
     * 연결된 OAuth Provider 목록
     */
    val connectedProviders: Set<OAuthProvider>
        get() = _oauthConnections.map { it.provider }.toSet()
    // endregion

    // region OAuth methods

    /**
     * 새로운 OAuth Provider를 연결합니다.
     *
     * @throws OAuthProviderAlreadyConnectedException 이미 연결된 Provider인 경우
     */
    fun connectOAuth(
        provider: OAuthProvider,
        providerId: String,
    ) {
        if (hasProvider(provider)) {
            throw OAuthProviderAlreadyConnectedException(provider.name)
        }

        val connection =
            OAuthConnection.create(
                provider = provider,
                providerId = providerId,
            )
        _oauthConnections.add(connection)
        touch()
        registerEvent(OAuthConnectedEvent(this, connection))
    }

    /**
     * OAuth Provider 연결을 해제합니다.
     * 비밀번호가 설정되어 있으면 마지막 OAuth도 해제할 수 있습니다.
     *
     * @throws OAuthProviderNotConnectedException 연결되지 않은 Provider인 경우
     * @throws CannotDisconnectLastLoginMethodException 해제 시 로그인할 수단이 없는 경우
     */
    fun disconnectOAuth(provider: OAuthProvider) {
        val connection =
            _oauthConnections.find { it.provider == provider }
                ?: throw OAuthProviderNotConnectedException(provider.name)

        // 마지막 OAuth이고 비밀번호가 없으면 해제 불가
        val isLastOAuth = _oauthConnections.size == 1
        if (isLastOAuth && !hasPassword) {
            throw CannotDisconnectLastLoginMethodException()
        }

        _oauthConnections.remove(connection)
        touch()
        registerEvent(OAuthDisconnectedEvent(this, provider))
    }

    /**
     * 특정 Provider가 연결되어 있는지 확인합니다.
     */
    fun hasProvider(provider: OAuthProvider): Boolean = _oauthConnections.any { it.provider == provider }

    /**
     * Provider ID로 OAuth 연결을 찾습니다.
     */
    fun findConnectionByProviderId(
        provider: OAuthProvider,
        providerId: String,
    ): OAuthConnection? = _oauthConnections.find { it.provider == provider && it.providerId == providerId }
    // endregion

    // region Profile methods

    /**
     * 프로필을 업데이트합니다.
     */
    fun updateProfile(
        newNickname: String? = null,
        newProfileImageUrl: String? = null,
    ) {
        var isUpdated = false

        newNickname?.let {
            if (nickname != it) {
                require(it.isNotBlank()) { "Nickname cannot be blank" }
                nickname = it
                isUpdated = true
            }
        }
        newProfileImageUrl?.let {
            if (profileImageUrl != it) {
                profileImageUrl = it
                isUpdated = true
            }
        }

        if (isUpdated) {
            touch()
            registerEvent(MemberProfileUpdatedEvent(this))
        }
    }
    // endregion

    // region Password & Email Verification methods

    /**
     * 비밀번호를 설정합니다.
     * OAuth로 가입한 사용자가 비밀번호를 추가로 설정할 때 사용합니다.
     *
     * @param newPasswordHash Application Layer에서 해시된 비밀번호
     */
    fun setPassword(newPasswordHash: PasswordHash) {
        passwordHash = newPasswordHash
        touch()
        registerEvent(PasswordSetEvent(this))
    }

    /**
     * 비밀번호를 변경합니다.
     *
     * @param newPasswordHash Application Layer에서 해시된 새 비밀번호
     * @throws PasswordNotSetException 비밀번호가 설정되지 않은 경우
     */
    fun changePassword(newPasswordHash: PasswordHash) {
        if (!hasPassword) {
            throw PasswordNotSetException()
        }

        passwordHash = newPasswordHash
        touch()
        registerEvent(PasswordChangedEvent(this))
    }

    /**
     * 이메일 인증을 완료합니다.
     */
    fun verifyEmail() {
        if (emailVerified) return

        emailVerified = true
        touch()
        registerEvent(EmailVerifiedEvent(this))
    }
    // endregion

    companion object {
        /**
         * 이메일/비밀번호로 새로운 Member를 생성합니다.
         * ULID 기반 ID가 즉시 할당되어 DB 저장 전에도 참조 가능합니다.
         *
         * @param passwordHash Application Layer에서 해시된 비밀번호
         */
        fun createWithEmail(
            email: Email,
            nickname: String,
            passwordHash: PasswordHash,
            profileImageUrl: String? = null,
        ): Member {
            val now = LocalDateTime.now()
            val member =
                Member(
                    id = MemberId.new(),
                    email = email,
                    nickname = nickname,
                    profileImageUrl = profileImageUrl,
                    passwordHash = passwordHash,
                    emailVerified = false,
                    oauthConnections = emptyList(),
                    role = Role.USER,
                    version = null,
                    createdAt = now,
                    updatedAt = now,
                )
            member.registerEvent(MemberRegisteredEvent(member))
            return member
        }

        /**
         * OAuth로 새로운 Member를 생성합니다.
         * ULID 기반 ID가 즉시 할당되어 DB 저장 전에도 참조 가능합니다.
         * OAuth 가입 시 이메일은 자동으로 인증된 것으로 간주합니다.
         */
        fun createWithOAuth(
            email: Email,
            nickname: String,
            provider: OAuthProvider,
            providerId: String,
            profileImageUrl: String? = null,
        ): Member {
            val now = LocalDateTime.now()
            val initialConnection =
                OAuthConnection.create(
                    provider = provider,
                    providerId = providerId,
                )

            val member =
                Member(
                    id = MemberId.new(),
                    email = email,
                    nickname = nickname,
                    profileImageUrl = profileImageUrl,
                    passwordHash = null,
                    emailVerified = true,
                    oauthConnections = listOf(initialConnection),
                    role = Role.USER,
                    version = null,
                    createdAt = now,
                    updatedAt = now,
                )
            member.registerEvent(MemberRegisteredEvent(member))
            return member
        }
    }
}
