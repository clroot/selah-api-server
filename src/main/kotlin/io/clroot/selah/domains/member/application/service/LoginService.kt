package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.*
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.exception.EmailNotVerifiedException
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 로그인 서비스
 */
@Service
@Transactional
class LoginService(
    private val loadMemberPort: LoadMemberPort,
    private val sessionPort: SessionPort,
    private val passwordHashPort: PasswordHashPort,
    private val registerMemberUseCase: RegisterMemberUseCase,
) : LoginUseCase {
    override suspend fun loginWithEmail(command: LoginWithEmailCommand): LoginResult {
        // 회원 조회
        val member =
            loadMemberPort.findByEmail(command.email)
                ?: throw InvalidCredentialsException()

        // 비밀번호 검증
        val passwordHash =
            member.passwordHash
                ?: throw InvalidCredentialsException()

        if (!passwordHashPort.verify(command.password, passwordHash)) {
            throw InvalidCredentialsException()
        }

        // 이메일 인증 확인
        if (!member.emailVerified) {
            throw EmailNotVerifiedException(member.email.value)
        }

        // 세션 생성
        val session =
            sessionPort.create(
                memberId = member.id,
                role = member.role,
                userAgent = command.userAgent,
                ipAddress = command.ipAddress,
            )

        return LoginResult(
            session = session,
            memberId = member.id,
            nickname = member.nickname,
            isNewMember = false,
        )
    }

    override suspend fun loginWithOAuth(command: LoginWithOAuthCommand): LoginResult {
        // 회원가입 또는 로그인 처리
        val registerResult =
            registerMemberUseCase.registerOrLoginWithOAuth(
                RegisterWithOAuthCommand(
                    email = command.email,
                    nickname = command.nickname,
                    provider = command.provider,
                    providerId = command.providerId,
                    profileImageUrl = command.profileImageUrl,
                ),
            )

        val member = registerResult.member

        // 세션 생성
        val session =
            sessionPort.create(
                memberId = member.id,
                role = member.role,
                userAgent = command.userAgent,
                ipAddress = command.ipAddress,
            )

        return LoginResult(
            session = session,
            memberId = member.id,
            nickname = member.nickname,
            isNewMember = registerResult.isNewMember,
        )
    }
}
