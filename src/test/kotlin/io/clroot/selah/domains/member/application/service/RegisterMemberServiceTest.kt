package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.RegisterWithEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.RegisterWithOAuthCommand
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.*
import io.clroot.selah.domains.member.domain.exception.EmailAlreadyExistsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

class RegisterMemberServiceTest : DescribeSpec({

    val loadMemberPort = mockk<LoadMemberPort>()
    val saveMemberPort = mockk<SaveMemberPort>()
    val passwordHashPort = mockk<PasswordHashPort>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val registerMemberService = RegisterMemberService(
        loadMemberPort = loadMemberPort,
        saveMemberPort = saveMemberPort,
        passwordHashPort = passwordHashPort,
        eventPublisher = eventPublisher,
    )

    beforeTest {
        clearMocks(loadMemberPort, saveMemberPort, passwordHashPort)
    }

    describe("registerWithEmail") {
        val email = Email("test@example.com")
        val nickname = "TestUser"
        val password = NewPassword.from("Password123!")
        val passwordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$hashedvalue")

        val command = RegisterWithEmailCommand(
            email = email,
            nickname = nickname,
            password = password,
        )

        context("유효한 정보로 회원가입할 때") {
            it("새 회원이 생성되고 저장된다") {
                coEvery { loadMemberPort.existsByEmail(email) } returns false
                coEvery { passwordHashPort.hash(password) } returns passwordHash
                coEvery { saveMemberPort.save(any()) } answers {
                    firstArg<Member>().also { member ->
                        // 저장 시 version 등 메타데이터가 갱신될 수 있음
                    }
                }

                val result = registerMemberService.registerWithEmail(command)

                result shouldNotBe null
                result.email shouldBe email
                result.nickname shouldBe nickname
                result.passwordHash shouldBe passwordHash
                result.emailVerified shouldBe false
                result.role shouldBe Member.Role.USER
                result.hasPassword shouldBe true
                result.hasOAuthConnection shouldBe false

                coVerify(exactly = 1) { loadMemberPort.existsByEmail(email) }
                coVerify(exactly = 1) { passwordHashPort.hash(password) }
                coVerify(exactly = 1) { saveMemberPort.save(any()) }
            }
        }

        context("이미 존재하는 이메일로 회원가입할 때") {
            it("EmailAlreadyExistsException을 던진다") {
                coEvery { loadMemberPort.existsByEmail(email) } returns true

                val exception = shouldThrow<EmailAlreadyExistsException> {
                    registerMemberService.registerWithEmail(command)
                }

                exception.message shouldBe "Email already exists: ${email.value}"

                coVerify(exactly = 1) { loadMemberPort.existsByEmail(email) }
                coVerify(exactly = 0) { saveMemberPort.save(any()) }
            }
        }
    }

    describe("registerOrLoginWithOAuth") {
        val email = Email("oauth@example.com")
        val nickname = "OAuthUser"
        val provider = OAuthProvider.GOOGLE
        val providerId = "google-user-id-123"
        val profileImageUrl = "https://example.com/profile.jpg"

        val command = RegisterWithOAuthCommand(
            email = email,
            nickname = nickname,
            provider = provider,
            providerId = providerId,
            profileImageUrl = profileImageUrl,
        )

        context("기존 OAuth 연결이 있는 회원이 로그인할 때") {
            val existingMember = createOAuthMember(
                email = email,
                nickname = "ExistingUser",
                provider = provider,
                providerId = providerId,
            )

            it("기존 회원을 반환하고 isNewMember는 false다") {
                coEvery {
                    loadMemberPort.findByOAuthConnection(provider, providerId)
                } returns existingMember

                val result = registerMemberService.registerOrLoginWithOAuth(command)

                result.member shouldBe existingMember
                result.isNewMember shouldBe false

                coVerify(exactly = 1) { loadMemberPort.findByOAuthConnection(provider, providerId) }
                coVerify(exactly = 0) { saveMemberPort.save(any()) }
            }
        }

        context("동일 이메일의 기존 회원이 새로운 OAuth Provider로 로그인할 때") {
            val existingMember = createEmailMember(
                email = email,
                nickname = "EmailUser",
            )

            it("기존 회원에 OAuth 연결을 추가하고 isNewMember는 false다") {
                coEvery {
                    loadMemberPort.findByOAuthConnection(provider, providerId)
                } returns null
                coEvery { loadMemberPort.findByEmail(email) } returns existingMember
                coEvery { saveMemberPort.save(any()) } answers { firstArg() }

                val result = registerMemberService.registerOrLoginWithOAuth(command)

                result.member.email shouldBe email
                result.member.hasProvider(provider) shouldBe true
                result.isNewMember shouldBe false

                coVerify(exactly = 1) { loadMemberPort.findByOAuthConnection(provider, providerId) }
                coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                coVerify(exactly = 1) { saveMemberPort.save(any()) }
            }
        }

        context("완전히 새로운 사용자가 OAuth로 가입할 때") {
            it("신규 회원을 생성하고 isNewMember는 true다") {
                coEvery {
                    loadMemberPort.findByOAuthConnection(provider, providerId)
                } returns null
                coEvery { loadMemberPort.findByEmail(email) } returns null
                coEvery { saveMemberPort.save(any()) } answers { firstArg() }

                val result = registerMemberService.registerOrLoginWithOAuth(command)

                result.member.email shouldBe email
                result.member.nickname shouldBe nickname
                result.member.profileImageUrl shouldBe profileImageUrl
                result.member.hasProvider(provider) shouldBe true
                result.member.emailVerified shouldBe true
                result.isNewMember shouldBe true

                coVerify(exactly = 1) { loadMemberPort.findByOAuthConnection(provider, providerId) }
                coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                coVerify(exactly = 1) { saveMemberPort.save(any()) }
            }
        }
    }
})

// region Test Fixtures

private fun createEmailMember(
    email: Email,
    nickname: String = "EmailUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = MemberId.new(),
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = PasswordHash.from($$"$argon2id$hashedvalue"),
        emailVerified = true,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createOAuthMember(
    email: Email,
    nickname: String = "OAuthUser",
    provider: OAuthProvider = OAuthProvider.GOOGLE,
    providerId: String = "provider-id-123",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = MemberId.new(),
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = null,
        emailVerified = true,
        oauthConnections = listOf(
            OAuthConnection.create(
                provider = provider,
                providerId = providerId,
            )
        ),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

// endregion
