package io.clroot.selah.domains.member.adapter.outbound.security

import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.domain.Password
import io.clroot.selah.domains.member.domain.PasswordHash
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Argon2를 사용한 PasswordHashPort 구현
 *
 * Spring Security의 Argon2PasswordEncoder를 활용합니다.
 * Argon2는 2015년 Password Hashing Competition 우승 알고리즘으로,
 * 메모리 집약적 설계로 GPU/ASIC 공격에 강합니다.
 */
@Component
class PasswordHashAdapter(
    private val passwordEncoder: PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
) : PasswordHashPort {
    override fun hash(password: Password): PasswordHash {
        val hashedValue = passwordEncoder.encode(password.value)
            ?: throw IllegalStateException("Password encoding failed")
        return PasswordHash.from(hashedValue)
    }

    override fun verify(password: Password, hash: PasswordHash): Boolean {
        return passwordEncoder.matches(password.value, hash.value)
    }
}
