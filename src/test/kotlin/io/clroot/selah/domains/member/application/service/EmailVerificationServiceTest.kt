package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.SendVerificationEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.VerifyEmailCommand
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.member.domain.exception.EmailAlreadyVerifiedException
import io.clroot.selah.domains.member.domain.exception.EmailVerificationResendTooSoonException
import io.clroot.selah.domains.member.domain.exception.InvalidEmailVerificationTokenException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationServiceTest :
    DescribeSpec({

        lateinit var loadMemberPort: LoadMemberPort
        lateinit var saveMemberPort: SaveMemberPort
        lateinit var emailVerificationTokenPort: EmailVerificationTokenPort
        lateinit var sendEmailPort: SendEmailPort
        lateinit var eventPublisher: ApplicationEventPublisher
        lateinit var testScope: TestScope
        lateinit var emailVerificationService: EmailVerificationService

        val resendCooldown = Duration.ofMinutes(5)

        beforeEach {
            clearAllMocks()
            loadMemberPort = mockk()
            saveMemberPort = mockk()
            emailVerificationTokenPort = mockk()
            sendEmailPort = mockk()
            eventPublisher = mockk(relaxed = true)
            testScope = TestScope()

            emailVerificationService =
                EmailVerificationService(
                    loadMemberPort = loadMemberPort,
                    saveMemberPort = saveMemberPort,
                    emailVerificationTokenPort = emailVerificationTokenPort,
                    sendEmailPort = sendEmailPort,
                    eventPublisher = eventPublisher,
                    applicationScope = testScope,
                    resendCooldown = resendCooldown,
                )
        }

        describe("sendVerificationEmail") {
            val memberId = MemberId.new()
            val email = Email("test@example.com")
            val command = SendVerificationEmailCommand(memberId)

            context("이메일이 아직 인증되지 않은 회원인 경우") {
                val member = createUnverifiedMember(id = memberId, email = email)
                val tokenResult = createTokenResult(memberId)

                it("인증 이메일을 발송한다") {
                    coEvery { loadMemberPort.findById(memberId) } returns member
                    coEvery { emailVerificationTokenPort.findLatestCreatedAtByMemberId(memberId) } returns null
                    coEvery { emailVerificationTokenPort.invalidateAllByMemberId(memberId) } just Runs
                    coEvery { emailVerificationTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendVerificationEmail(email, member.nickname, tokenResult.rawToken)
                    } just Runs

                    emailVerificationService.sendVerificationEmail(command)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                    coVerify(exactly = 1) { emailVerificationTokenPort.invalidateAllByMemberId(memberId) }
                    coVerify(exactly = 1) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendVerificationEmail(email, member.nickname, tokenResult.rawToken)
                    }
                }
            }

            context("회원이 존재하지 않는 경우") {
                it("MemberNotFoundException을 던진다") {
                    coEvery { loadMemberPort.findById(memberId) } returns null

                    val exception =
                        shouldThrow<MemberNotFoundException> {
                            emailVerificationService.sendVerificationEmail(command)
                        }

                    exception.message shouldBe "Member not found: ${memberId.value}"
                    coVerify(exactly = 0) { emailVerificationTokenPort.create(memberId) }
                }
            }

            context("이미 이메일이 인증된 회원인 경우") {
                it("EmailAlreadyVerifiedException을 던진다") {
                    val verifiedMember = createVerifiedMember(id = memberId, email = email)
                    coEvery { loadMemberPort.findById(memberId) } returns verifiedMember

                    val exception =
                        shouldThrow<EmailAlreadyVerifiedException> {
                            emailVerificationService.sendVerificationEmail(command)
                        }

                    exception.message shouldBe "Email already verified: ${email.value}"
                    coVerify(exactly = 0) { emailVerificationTokenPort.create(memberId) }
                }
            }

            context("재발송 쿨다운 시간 내인 경우") {
                it("EmailVerificationResendTooSoonException을 던진다") {
                    val member = createUnverifiedMember(id = memberId, email = email)
                    val recentCreatedAt = LocalDateTime.now().minusMinutes(2) // 2분 전 (5분 쿨다운 내)

                    coEvery { loadMemberPort.findById(memberId) } returns member
                    coEvery {
                        emailVerificationTokenPort.findLatestCreatedAtByMemberId(memberId)
                    } returns recentCreatedAt

                    val exception =
                        shouldThrow<EmailVerificationResendTooSoonException> {
                            emailVerificationService.sendVerificationEmail(command)
                        }

                    // 남은 시간은 약 3분이어야 함 (정확한 값은 테스트 실행 시점에 따라 다를 수 있음)
                    (exception.remainingSeconds in 170L..190L) shouldBe true
                    coVerify(exactly = 0) { emailVerificationTokenPort.create(memberId) }
                }
            }

            context("재발송 쿨다운이 지난 경우") {
                it("인증 이메일을 발송한다") {
                    val member = createUnverifiedMember(id = memberId, email = email)
                    val oldCreatedAt = LocalDateTime.now().minusMinutes(10) // 10분 전 (쿨다운 지남)
                    val tokenResult = createTokenResult(memberId)

                    coEvery { loadMemberPort.findById(memberId) } returns member
                    coEvery {
                        emailVerificationTokenPort.findLatestCreatedAtByMemberId(memberId)
                    } returns oldCreatedAt
                    coEvery { emailVerificationTokenPort.invalidateAllByMemberId(memberId) } just Runs
                    coEvery { emailVerificationTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendVerificationEmail(email, member.nickname, tokenResult.rawToken)
                    } just Runs

                    emailVerificationService.sendVerificationEmail(command)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 1) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendVerificationEmail(email, member.nickname, tokenResult.rawToken)
                    }
                }
            }
        }

        describe("verifyEmail") {
            val memberId = MemberId.new()
            val email = Email("test@example.com")
            val rawToken = "valid-token-12345"
            val tokenId = "token-id-12345"
            val command = VerifyEmailCommand(rawToken)

            context("유효한 토큰으로 인증 요청 시") {
                it("이메일 인증을 완료하고 회원 정보를 반환한다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)
                    val unverifiedMember = createUnverifiedMember(id = memberId, email = email)
                    val verifiedMember = createVerifiedMember(id = memberId, email = email)

                    coEvery { emailVerificationTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { emailVerificationTokenPort.markAsUsed(tokenId) } just Runs
                    coEvery { loadMemberPort.findById(memberId) } returns unverifiedMember
                    coEvery { saveMemberPort.save(any()) } returns verifiedMember

                    val result = emailVerificationService.verifyEmail(command)

                    result.emailVerified shouldBe true
                    result.id shouldBe memberId

                    coVerify(exactly = 1) { emailVerificationTokenPort.findValidByToken(rawToken) }
                    coVerify(exactly = 1) { emailVerificationTokenPort.markAsUsed(tokenId) }
                    coVerify(exactly = 1) { loadMemberPort.findById(memberId) }
                    coVerify(exactly = 1) { saveMemberPort.save(any()) }
                }
            }

            context("유효하지 않은 토큰으로 인증 요청 시") {
                it("InvalidEmailVerificationTokenException을 던진다") {
                    coEvery { emailVerificationTokenPort.findValidByToken(rawToken) } returns null

                    shouldThrow<InvalidEmailVerificationTokenException> {
                        emailVerificationService.verifyEmail(command)
                    }

                    coVerify(exactly = 1) { emailVerificationTokenPort.findValidByToken(rawToken) }
                    coVerify(exactly = 0) { emailVerificationTokenPort.markAsUsed(tokenId) }
                }
            }

            context("토큰은 유효하지만 회원이 존재하지 않는 경우") {
                it("MemberNotFoundException을 던진다") {
                    val tokenInfo = createTokenInfo(id = tokenId, memberId = memberId)

                    coEvery { emailVerificationTokenPort.findValidByToken(rawToken) } returns tokenInfo
                    coEvery { emailVerificationTokenPort.markAsUsed(tokenId) } just Runs
                    coEvery { loadMemberPort.findById(memberId) } returns null

                    val exception =
                        shouldThrow<MemberNotFoundException> {
                            emailVerificationService.verifyEmail(command)
                        }

                    exception.message shouldBe "Member not found: ${memberId.value}"
                }
            }
        }
    })

// region Test Fixtures

private fun createUnverifiedMember(
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
        passwordHash = PasswordHash.from("${"$"}argon2id${"$"}v=19${"$"}m=65536,t=3,p=4${"$"}hashedvalue"),
        emailVerified = false,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createVerifiedMember(
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
        passwordHash = PasswordHash.from("${"$"}argon2id${"$"}v=19${"$"}m=65536,t=3,p=4${"$"}hashedvalue"),
        emailVerified = true,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createTokenResult(memberId: MemberId): EmailVerificationTokenCreateResult {
    val now = LocalDateTime.now()
    return EmailVerificationTokenCreateResult(
        info =
            EmailVerificationTokenInfo(
                id = "token-id-${System.currentTimeMillis()}",
                memberId = memberId,
                expiresAt = now.plusDays(1),
                usedAt = null,
                createdAt = now,
            ),
        rawToken = "raw-token-${System.currentTimeMillis()}",
    )
}

private fun createTokenInfo(
    id: String = "token-id-12345",
    memberId: MemberId = MemberId.new(),
): EmailVerificationTokenInfo {
    val now = LocalDateTime.now()
    return EmailVerificationTokenInfo(
        id = id,
        memberId = memberId,
        expiresAt = now.plusDays(1),
        usedAt = null,
        createdAt = now,
    )
}

// endregion
