package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.RequestPasswordResetCommand
import io.clroot.selah.domains.member.application.port.inbound.ResetPasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.ValidateResetTokenCommand
import io.clroot.selah.domains.member.application.port.outbound.*
import io.clroot.selah.domains.member.domain.*
import io.clroot.selah.domains.member.domain.exception.InvalidPasswordResetTokenException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.PasswordResetResendTooSoonException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.Duration
import java.time.LocalDateTime

class PasswordResetServiceTest :
    DescribeSpec({

        val loadMemberPort = mockk<LoadMemberPort>()
        val saveMemberPort = mockk<SaveMemberPort>()
        val passwordResetTokenPort = mockk<PasswordResetTokenPort>()
        val passwordHashPort = mockk<PasswordHashPort>()
        val sessionPort = mockk<SessionPort>()
        val sendEmailPort = mockk<SendEmailPort>()

        val resendCooldown = Duration.ofMinutes(1)

        val passwordResetService =
            PasswordResetService(
                loadMemberPort = loadMemberPort,
                saveMemberPort = saveMemberPort,
                passwordResetTokenPort = passwordResetTokenPort,
                passwordHashPort = passwordHashPort,
                sessionPort = sessionPort,
                sendEmailPort = sendEmailPort,
                resendCooldown = resendCooldown,
            )

        beforeTest {
            clearAllMocks()
        }

        describe("requestPasswordReset") {
            val email = Email("test@example.com")
            val command = RequestPasswordResetCommand(email)

            context("회원이 존재하는 경우") {
                val memberId = MemberId.new()
                val member = createMember(id = memberId, email = email)
                val tokenResult = createTokenResult(memberId)

                it("비밀번호 재설정 이메일을 발송한다") {
                    coEvery { loadMemberPort.findByEmail(email) } returns member
                    coEvery { passwordResetTokenPort.findLatestCreatedAtByMemberId(memberId) } returns null
                    coEvery { passwordResetTokenPort.invalidateAllByMemberId(memberId) } just Runs
                    coEvery { passwordResetTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendPasswordResetEmail(email, member.nickname, tokenResult.rawToken)
                    } just Runs

                    passwordResetService.requestPasswordReset(command)

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                    coVerify(exactly = 1) { passwordResetTokenPort.invalidateAllByMemberId(memberId) }
                    coVerify(exactly = 1) { passwordResetTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendPasswordResetEmail(email, member.nickname, tokenResult.rawToken)
                    }
                }
            }

            context("회원이 존재하지 않는 경우") {
                it("조용히 무시한다 (보안)") {
                    coEvery { loadMemberPort.findByEmail(email) } returns null

                    // 예외를 던지지 않음
                    passwordResetService.requestPasswordReset(command)

                    coVerify(exactly = 1) { loadMemberPort.findByEmail(email) }
                    // 회원이 없으면 토큰 생성/이메일 발송이 호출되지 않음
                }
            }

            context("재발송 쿨다운 시간 내인 경우") {
                it("PasswordResetResendTooSoonException을 던진다") {
                    val memberId = MemberId.new()
                    val member = createMember(id = memberId, email = email)
                    val recentCreatedAt = LocalDateTime.now().minusSeconds(30) // 30초 전 (1분 쿨다운 내)

                    coEvery { loadMemberPort.findByEmail(email) } returns member
                    coEvery {
                        passwordResetTokenPort.findLatestCreatedAtByMemberId(memberId)
                    } returns recentCreatedAt

                    val exception =
                        shouldThrow<PasswordResetResendTooSoonException> {
                            passwordResetService.requestPasswordReset(command)
                        }

                    // 남은 시간은 약 30초이어야 함
                    (exception.remainingSeconds in 25L..35L) shouldBe true
                    // 쿨다운 중이면 토큰 생성이 호출되지 않음
                }
            }

            context("재발송 쿨다운이 지난 경우") {
                val memberId = MemberId.new()
                val member = createMember(id = memberId, email = email)
                val tokenResult = createTokenResult(memberId)

                it("비밀번호 재설정 이메일을 발송한다") {
                    val oldCreatedAt = LocalDateTime.now().minusMinutes(5) // 5분 전 (쿨다운 지남)

                    coEvery { loadMemberPort.findByEmail(email) } returns member
                    coEvery {
                        passwordResetTokenPort.findLatestCreatedAtByMemberId(memberId)
                    } returns oldCreatedAt
                    coEvery { passwordResetTokenPort.invalidateAllByMemberId(memberId) } just Runs
                    coEvery { passwordResetTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendPasswordResetEmail(email, member.nickname, tokenResult.rawToken)
                    } just Runs

                    passwordResetService.requestPasswordReset(command)

                    coVerify(exactly = 1) { passwordResetTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendPasswordResetEmail(email, member.nickname, tokenResult.rawToken)
                    }
                }
            }
        }

        describe("validateResetToken") {
            val memberId = MemberId.new()
            val email = Email("test@example.com")
            val rawToken = "valid-token-12345"
            val tokenId = "token-id-12345"
            val command = ValidateResetTokenCommand(rawToken)

            context("유효한 토큰인 경우") {
                it("마스킹된 이메일과 함께 유효함을 반환한다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)
                    val member = createMember(id = memberId, email = email)

                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { loadMemberPort.findById(memberId) } returns member

                    val result = passwordResetService.validateResetToken(command)

                    result.valid shouldBe true
                    result.maskedEmail shouldBe "te**@example.com"

                    coVerify(exactly = 1) { passwordResetTokenPort.findValidByToken(rawToken) }
                    coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                }
            }

            context("유효하지 않은 토큰인 경우") {
                it("InvalidPasswordResetTokenException을 던진다") {
                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns null

                    shouldThrow<InvalidPasswordResetTokenException> {
                        passwordResetService.validateResetToken(command)
                    }

                    coVerify(exactly = 1) { passwordResetTokenPort.findValidByToken(rawToken) }
                    // 토큰이 없으면 회원 조회가 호출되지 않음
                }
            }

            context("토큰은 유효하지만 회원이 존재하지 않는 경우") {
                it("MemberNotFoundException을 던진다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)

                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { loadMemberPort.findById(memberId) } returns null

                    val exception =
                        shouldThrow<MemberNotFoundException> {
                            passwordResetService.validateResetToken(command)
                        }

                    exception.message shouldBe "Member not found: ${memberId.value}"
                }
            }
        }

        describe("resetPassword") {
            val memberId = MemberId.new()
            val email = Email("test@example.com")
            val rawToken = "valid-token-12345"
            val tokenId = "token-id-12345"
            val newPasswordValue = "NewP@ssw0rd123!"
            val command = ResetPasswordCommand(rawToken, newPasswordValue)

            context("유효한 토큰과 비밀번호로 재설정 요청 시") {
                it("비밀번호를 변경하고 세션을 무효화하며 알림 이메일을 발송한다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)
                    val member = createMember(id = memberId, email = email)
                    val newPasswordHash = PasswordHash.from($$"$argon2id$v=19$m=65536,t=3,p=4$newhashedvalue")

                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { loadMemberPort.findById(memberId) } returns member
                    every { passwordHashPort.hash(any<NewPassword>()) } returns newPasswordHash
                    coEvery { saveMemberPort.save(any()) } returns member
                    coEvery { passwordResetTokenPort.markAsUsed(tokenId) } just Runs
                    coEvery { sessionPort.deleteAllByMemberId(memberId) } just Runs
                    coEvery {
                        sendEmailPort.sendPasswordChangedNotification(email, member.nickname)
                    } just Runs

                    passwordResetService.resetPassword(command)

                    coVerify(exactly = 1) { passwordResetTokenPort.findValidByToken(rawToken) }
                    coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                    coVerify(exactly = 1) { passwordHashPort.hash(any<NewPassword>()) }
                    coVerify(exactly = 1) { saveMemberPort.save(any()) }
                    coVerify(exactly = 1) { passwordResetTokenPort.markAsUsed(tokenId) }
                    coVerify(exactly = 1) { sessionPort.deleteAllByMemberId(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendPasswordChangedNotification(email, member.nickname)
                    }
                }
            }

            context("유효하지 않은 토큰으로 재설정 요청 시") {
                it("InvalidPasswordResetTokenException을 던진다") {
                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns null

                    shouldThrow<InvalidPasswordResetTokenException> {
                        passwordResetService.resetPassword(command)
                    }

                    // 토큰이 없으면 비밀번호 변경이 진행되지 않음
                }
            }

            context("비밀번호 정책을 충족하지 않는 경우") {
                it("IllegalArgumentException을 던진다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)
                    val weakPasswordCommand = ResetPasswordCommand(rawToken, "weak")

                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns tokenInfo

                    shouldThrow<IllegalArgumentException> {
                        passwordResetService.resetPassword(weakPasswordCommand)
                    }

                    // 비밀번호 정책 검증 실패 시 비밀번호 변경이 진행되지 않음
                }
            }

            context("토큰은 유효하지만 회원이 존재하지 않는 경우") {
                it("MemberNotFoundException을 던진다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)

                    coEvery { passwordResetTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { loadMemberPort.findById(memberId) } returns null

                    val exception =
                        shouldThrow<MemberNotFoundException> {
                            passwordResetService.resetPassword(command)
                        }

                    exception.message shouldBe "Member not found: ${memberId.value}"
                    // 회원이 없으면 비밀번호 변경이 진행되지 않음
                }
            }
        }
    })

// region Test Fixtures

private fun createMember(
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

private fun createTokenResult(memberId: MemberId): PasswordResetTokenCreateResult {
    val now = LocalDateTime.now()
    return PasswordResetTokenCreateResult(
        info =
            PasswordResetTokenInfo(
                id = "token-id-${System.currentTimeMillis()}",
                memberId = memberId,
                expiresAt = now.plusHours(1),
                usedAt = null,
                createdAt = now,
            ),
        rawToken = "raw-token-${System.currentTimeMillis()}",
    )
}

private fun createTokenInfo(
    id: String = "token-id-12345",
    memberId: MemberId = MemberId.new(),
): PasswordResetTokenInfo {
    val now = LocalDateTime.now()
    return PasswordResetTokenInfo(
        id = id,
        memberId = memberId,
        expiresAt = now.plusHours(1),
        usedAt = null,
        createdAt = now,
    )
}

// endregion
