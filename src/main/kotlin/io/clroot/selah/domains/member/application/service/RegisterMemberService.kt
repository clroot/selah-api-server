package io.clroot.selah.domains.member.application.service

import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.application.port.inbound.*
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.exception.EmailAlreadyExistsException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원가입 서비스
 */
@Service
@Transactional
class RegisterMemberService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val passwordHashPort: PasswordHashPort,
    private val eventPublisher: ApplicationEventPublisher,
) : RegisterMemberUseCase {

    override suspend fun registerWithEmail(command: RegisterWithEmailCommand): Member {
        // 이메일 중복 검사
        if (loadMemberPort.existsByEmail(command.email)) {
            throw EmailAlreadyExistsException(command.email.value)
        }

        // 비밀번호 해싱
        val passwordHash = passwordHashPort.hash(command.password)

        // Member 생성
        val member = Member.createWithEmail(
            email = command.email,
            nickname = command.nickname,
            passwordHash = passwordHash,
        )

        // 저장
        val savedMember = saveMemberPort.save(member)

        // 이벤트 발행
        savedMember.publishAndClearEvents(eventPublisher)

        return savedMember
    }

    override suspend fun registerOrLoginWithOAuth(command: RegisterWithOAuthCommand): OAuthRegisterResult {
        // 기존 OAuth 연결 확인
        val existingMember = loadMemberPort.findByOAuthConnection(
            command.provider,
            command.providerId,
        )

        if (existingMember != null) {
            // 기존 회원 - 로그인 처리
            return OAuthRegisterResult(
                member = existingMember,
                isNewMember = false,
            )
        }

        // 이메일로 기존 회원 확인
        val memberByEmail = loadMemberPort.findByEmail(command.email)

        if (memberByEmail != null) {
            // 기존 회원에게 OAuth 연결 추가
            memberByEmail.connectOAuth(
                provider = command.provider,
                providerId = command.providerId,
            )

            val savedMember = saveMemberPort.save(memberByEmail)
            savedMember.publishAndClearEvents(eventPublisher)

            return OAuthRegisterResult(
                member = savedMember,
                isNewMember = false,
            )
        }

        // 신규 회원 생성
        val newMember = Member.createWithOAuth(
            email = command.email,
            nickname = command.nickname,
            provider = command.provider,
            providerId = command.providerId,
            profileImageUrl = command.profileImageUrl,
        )

        val savedMember = saveMemberPort.save(newMember)
        savedMember.publishAndClearEvents(eventPublisher)

        return OAuthRegisterResult(
            member = savedMember,
            isNewMember = true,
        )
    }
}
