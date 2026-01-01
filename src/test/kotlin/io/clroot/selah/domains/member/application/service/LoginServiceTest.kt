package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.LoginWithEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.LoginWithOAuthCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthRegisterResult
import io.clroot.selah.domains.member.application.port.inbound.RegisterMemberUseCase
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.application.port.outbound.SessionInfo
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.*
import io.clroot.selah.domains.member.domain.exception.EmailNotVerifiedException
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class LoginServiceTest :
    DescribeSpec({

        val loadMemberPort = mockk<LoadMemberPort>()
        val sessionPort = mockk<SessionPort>()
        val passwordHashPort = mockk<PasswordHashPort>()
        val registerMemberUseCase = mockk<RegisterMemberUseCase>()

        val loginService =
            LoginService(
                loadMemberPort = loadMemberPort,
                sessionPort = sessionPort,
                passwordHashPort = passwordHashPort,
                registerMemberUseCase = registerMemberUseCase,
            )

        beforeTest {
            clearAllMocks()
        }

        describe("loginWithEmail") {
            val email = Email("test@example.com")
            val rawPassword = RawPassword("password123!")
            val passwordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$hashedvalue")
            val memberId = MemberId.new()
            val userAgent = "Mozilla/5.0"
            val ipAddress = "192.168.1.1"

            val command =
                LoginWithEmailCommand(
                    email = email,
                    password = rawPassword,
                    userAgent = userAgent,
                    ipAddress = ipAddress,
                )

            context("유효한 인증 정보로 로그인할 때") {
                val member =
                    createMember(
                        id = memberId,
                        email = email,
                        passwordHash = passwordHash,
                        emailVerified = true,
                    )

                val sessionInfo = createSessionInfo(memberId)

                it("로그인에 성공하고 세션을 반환한다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns member
                    coEvery { passwordHashPort.verify(rawPassword, passwordHash) } returns true
                    coEvery {
                        sessionPort.create(memberId, Member.Role.USER, userAgent, ipAddress)
                    } returns sessionInfo

                    val result = loginService.loginWithEmail(command)

                    result.session shouldBe sessionInfo
                    result.memberId shouldBe memberId
                    result.nickname shouldBe member.nickname
                    result.isNewMember shouldBe false

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                    coVerify(exactly = 1) { passwordHashPort.verify(rawPassword, passwordHash) }
                    coVerify(exactly = 1) { sessionPort.create(memberId, Member.Role.USER, userAgent, ipAddress) }
                }
            }

            context("존재하지 않는 이메일로 로그인할 때") {
                it("InvalidCredentialsException을 던진다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns null

                    shouldThrow<InvalidCredentialsException> {
                        loginService.loginWithEmail(command)
                    }

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                }
            }

            context("비밀번호가 없는 OAuth 전용 회원이 이메일 로그인을 시도할 때") {
                val oauthOnlyMember =
                    createOAuthOnlyMember(
                        id = memberId,
                        email = email,
                    )

                it("InvalidCredentialsException을 던진다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns oauthOnlyMember

                    shouldThrow<InvalidCredentialsException> {
                        loginService.loginWithEmail(command)
                    }

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                }
            }

            context("비밀번호가 일치하지 않을 때") {
                val member =
                    createMember(
                        id = memberId,
                        email = email,
                        passwordHash = passwordHash,
                        emailVerified = true,
                    )

                it("InvalidCredentialsException을 던진다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns member
                    coEvery { passwordHashPort.verify(rawPassword, passwordHash) } returns false

                    shouldThrow<InvalidCredentialsException> {
                        loginService.loginWithEmail(command)
                    }

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                    coVerify(exactly = 1) { passwordHashPort.verify(rawPassword, passwordHash) }
                }
            }

            context("이메일이 인증되지 않은 회원이 로그인할 때") {
                val unverifiedMember =
                    createMember(
                        id = memberId,
                        email = email,
                        passwordHash = passwordHash,
                        emailVerified = false,
                    )

                it("EmailNotVerifiedException을 던진다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns unverifiedMember
                    coEvery { passwordHashPort.verify(rawPassword, passwordHash) } returns true

                    val exception =
                        shouldThrow<EmailNotVerifiedException> {
                            loginService.loginWithEmail(command)
                        }

                    exception.message shouldBe "Email not verified: ${email.value}"

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                    coVerify(exactly = 1) { passwordHashPort.verify(rawPassword, passwordHash) }
                }
            }
        }

        describe("loginWithOAuth") {
            val email = Email("oauth@example.com")
            val nickname = "OAuthUser"
            val provider = OAuthProvider.GOOGLE
            val providerId = "google-user-id-123"
            val profileImageUrl = "https://example.com/profile.jpg"
            val userAgent = "Mozilla/5.0"
            val ipAddress = "192.168.1.1"

            val command =
                LoginWithOAuthCommand(
                    email = email,
                    nickname = nickname,
                    provider = provider,
                    providerId = providerId,
                    profileImageUrl = profileImageUrl,
                    userAgent = userAgent,
                    ipAddress = ipAddress,
                )

            context("신규 회원이 OAuth로 로그인할 때") {
                val memberId = MemberId.new()
                val newMember =
                    createOAuthOnlyMember(
                        id = memberId,
                        email = email,
                        nickname = nickname,
                    )

                val sessionInfo = createSessionInfo(memberId)

                it("회원가입 후 세션을 생성하고 isNewMember가 true다") {
                    coEvery {
                        registerMemberUseCase.registerOrLoginWithOAuth(any())
                    } returns OAuthRegisterResult(member = newMember, isNewMember = true)

                    coEvery {
                        sessionPort.create(memberId, Member.Role.USER, userAgent, ipAddress)
                    } returns sessionInfo

                    val result = loginService.loginWithOAuth(command)

                    result.session shouldBe sessionInfo
                    result.memberId shouldBe memberId
                    result.nickname shouldBe nickname
                    result.isNewMember shouldBe true

                    coVerify(exactly = 1) {
                        registerMemberUseCase.registerOrLoginWithOAuth(
                            match {
                                it.email == email &&
                                    it.nickname == nickname &&
                                    it.provider == provider &&
                                    it.providerId == providerId &&
                                    it.profileImageUrl == profileImageUrl
                            },
                        )
                    }
                    coVerify(exactly = 1) { sessionPort.create(memberId, Member.Role.USER, userAgent, ipAddress) }
                }
            }

            context("기존 회원이 OAuth로 로그인할 때") {
                val memberId = MemberId.new()
                val existingMember =
                    createOAuthOnlyMember(
                        id = memberId,
                        email = email,
                        nickname = "ExistingNickname",
                    )

                val sessionInfo = createSessionInfo(memberId)

                it("세션만 생성하고 isNewMember가 false다") {
                    coEvery {
                        registerMemberUseCase.registerOrLoginWithOAuth(any())
                    } returns OAuthRegisterResult(member = existingMember, isNewMember = false)

                    coEvery {
                        sessionPort.create(memberId, Member.Role.USER, userAgent, ipAddress)
                    } returns sessionInfo

                    val result = loginService.loginWithOAuth(command)

                    result.session shouldBe sessionInfo
                    result.memberId shouldBe memberId
                    result.nickname shouldBe existingMember.nickname
                    result.isNewMember shouldBe false
                }
            }
        }
    })

// region Test Fixtures

private fun createMember(
    id: MemberId,
    email: Email,
    nickname: String = "TestUser",
    passwordHash: PasswordHash,
    emailVerified: Boolean,
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = passwordHash,
        emailVerified = emailVerified,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createOAuthOnlyMember(
    id: MemberId,
    email: Email,
    nickname: String = "OAuthUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = null,
        emailVerified = true,
        oauthConnections =
            listOf(
                OAuthConnection.create(
                    provider = OAuthProvider.GOOGLE,
                    providerId = "google-id-123",
                ),
            ),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createSessionInfo(memberId: MemberId): SessionInfo {
    val now = LocalDateTime.now()
    return SessionInfo(
        token = "session-token-${System.currentTimeMillis()}",
        memberId = memberId,
        role = Member.Role.USER,
        userAgent = "Mozilla/5.0",
        createdIp = "192.168.1.1",
        lastAccessedIp = "192.168.1.1",
        expiresAt = now.plusDays(7),
        createdAt = now,
    )
}

// endregion
