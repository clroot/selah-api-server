package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.ChangePasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.SetPasswordCommand
import io.clroot.selah.domains.member.application.port.outbound.*
import io.clroot.selah.domains.member.domain.*
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.PasswordAlreadySetException
import io.clroot.selah.domains.member.domain.exception.PasswordNotSetException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import java.time.LocalDateTime

class PasswordServiceTest : DescribeSpec({

    val loadMemberPort = mockk<LoadMemberPort>()
    val saveMemberPort = mockk<SaveMemberPort>()
    val passwordHashPort = mockk<PasswordHashPort>()
    val sessionPort = mockk<SessionPort>()
    val sendEmailPort = mockk<SendEmailPort>()

    val passwordService = PasswordService(
        loadMemberPort = loadMemberPort,
        saveMemberPort = saveMemberPort,
        passwordHashPort = passwordHashPort,
        sessionPort = sessionPort,
        sendEmailPort = sendEmailPort,
    )

    beforeTest {
        clearAllMocks()
    }

    describe("changePassword") {
        val memberId = MemberId.new()
        val email = Email("test@example.com")
        val currentPassword = "CurrentP@ss1"
        val newPasswordValue = "NewP@ssw0rd123!"
        val command = ChangePasswordCommand(currentPassword, newPasswordValue)

        context("비밀번호가 있는 회원이 올바른 현재 비밀번호로 변경 요청 시") {
            it("비밀번호를 변경하고 세션을 무효화하며 알림 이메일을 발송한다") {
                val member = createMemberWithPassword(id = memberId, email = email)
                val newPasswordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$newhashedvalue")

                coEvery { loadMemberPort.findById(memberId) } returns member
                every { passwordHashPort.verify(any<RawPassword>(), any()) } returns true
                every { passwordHashPort.hash(any<NewPassword>()) } returns newPasswordHash
                coEvery { saveMemberPort.save(any()) } returns member
                coEvery { sessionPort.deleteAllByMemberId(memberId) } just Runs
                coEvery { sendEmailPort.sendPasswordChangedNotification(email, member.nickname) } just Runs

                passwordService.changePassword(memberId, command)

                coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                verify(exactly = 1) { passwordHashPort.verify(any<RawPassword>(), any()) }
                verify(exactly = 1) { passwordHashPort.hash(any<NewPassword>()) }
                coVerify(exactly = 1) { saveMemberPort.save(any()) }
                coVerify(exactly = 1) { sessionPort.deleteAllByMemberId(memberId) }
                coVerify(exactly = 1) { sendEmailPort.sendPasswordChangedNotification(email, member.nickname) }
            }
        }

        context("회원이 존재하지 않는 경우") {
            it("MemberNotFoundException을 던진다") {
                coEvery { loadMemberPort.findById(memberId) } returns null

                shouldThrow<MemberNotFoundException> {
                    passwordService.changePassword(memberId, command)
                }
            }
        }

        context("비밀번호가 설정되지 않은 OAuth 회원인 경우") {
            it("PasswordNotSetException을 던진다") {
                val oauthMember = createOAuthOnlyMember(id = memberId, email = email)
                coEvery { loadMemberPort.findById(memberId) } returns oauthMember

                shouldThrow<PasswordNotSetException> {
                    passwordService.changePassword(memberId, command)
                }
            }
        }

        context("현재 비밀번호가 일치하지 않는 경우") {
            it("InvalidCredentialsException을 던진다") {
                val member = createMemberWithPassword(id = memberId, email = email)

                coEvery { loadMemberPort.findById(memberId) } returns member
                every { passwordHashPort.verify(any<RawPassword>(), any()) } returns false

                shouldThrow<InvalidCredentialsException> {
                    passwordService.changePassword(memberId, command)
                }
            }
        }

        context("새 비밀번호가 정책을 충족하지 않는 경우") {
            it("IllegalArgumentException을 던진다") {
                val member = createMemberWithPassword(id = memberId, email = email)
                val weakPasswordCommand = ChangePasswordCommand(currentPassword, "weak")

                coEvery { loadMemberPort.findById(memberId) } returns member
                every { passwordHashPort.verify(any<RawPassword>(), any()) } returns true

                shouldThrow<IllegalArgumentException> {
                    passwordService.changePassword(memberId, weakPasswordCommand)
                }
            }
        }
    }

    describe("setPassword") {
        val memberId = MemberId.new()
        val email = Email("test@example.com")
        val newPasswordValue = "NewP@ssw0rd123!"
        val command = SetPasswordCommand(newPasswordValue)

        context("비밀번호가 없는 OAuth 회원이 비밀번호 설정 요청 시") {
            it("비밀번호를 설정하고 알림 이메일을 발송한다") {
                val oauthMember = createOAuthOnlyMember(id = memberId, email = email)
                val newPasswordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$newhashedvalue")

                coEvery { loadMemberPort.findById(memberId) } returns oauthMember
                every { passwordHashPort.hash(any<NewPassword>()) } returns newPasswordHash
                coEvery { saveMemberPort.save(any()) } returns oauthMember
                coEvery { sendEmailPort.sendPasswordChangedNotification(email, oauthMember.nickname) } just Runs

                passwordService.setPassword(memberId, command)

                coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                verify(exactly = 1) { passwordHashPort.hash(any<NewPassword>()) }
                coVerify(exactly = 1) { saveMemberPort.save(any()) }
                coVerify(exactly = 1) { sendEmailPort.sendPasswordChangedNotification(email, oauthMember.nickname) }
            }
        }

        context("회원이 존재하지 않는 경우") {
            it("MemberNotFoundException을 던진다") {
                coEvery { loadMemberPort.findById(memberId) } returns null

                shouldThrow<MemberNotFoundException> {
                    passwordService.setPassword(memberId, command)
                }
            }
        }

        context("이미 비밀번호가 설정된 회원인 경우") {
            it("PasswordAlreadySetException을 던진다") {
                val memberWithPassword = createMemberWithPassword(id = memberId, email = email)
                coEvery { loadMemberPort.findById(memberId) } returns memberWithPassword

                shouldThrow<PasswordAlreadySetException> {
                    passwordService.setPassword(memberId, command)
                }
            }
        }

        context("새 비밀번호가 정책을 충족하지 않는 경우") {
            it("IllegalArgumentException을 던진다") {
                val oauthMember = createOAuthOnlyMember(id = memberId, email = email)
                val weakPasswordCommand = SetPasswordCommand("weak")

                coEvery { loadMemberPort.findById(memberId) } returns oauthMember

                shouldThrow<IllegalArgumentException> {
                    passwordService.setPassword(memberId, weakPasswordCommand)
                }
            }
        }
    }
})

private fun createMemberWithPassword(
    id: MemberId = MemberId.new(),
    email: Email = Email("test@example.com"),
    nickname: String = "TestUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$hashedvalue"),
        emailVerified = true,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createOAuthOnlyMember(
    id: MemberId = MemberId.new(),
    email: Email = Email("test@example.com"),
    nickname: String = "TestUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = null,
        emailVerified = true,
        oauthConnections = listOf(
            OAuthConnection.create(
                provider = OAuthProvider.GOOGLE,
                providerId = "google-provider-id",
            ),
        ),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}
