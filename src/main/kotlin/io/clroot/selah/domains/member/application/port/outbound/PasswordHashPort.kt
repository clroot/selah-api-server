package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Password
import io.clroot.selah.domains.member.domain.PasswordHash

/**
 * 비밀번호 해싱을 위한 Outbound Port
 *
 * Domain Layer가 해싱 알고리즘(BCrypt 등)에 의존하지 않도록
 * Application Layer에서 추상화합니다.
 */
interface PasswordHashPort {
    /**
     * 평문 비밀번호를 해싱합니다.
     *
     * @param password 평문 비밀번호 (RawPassword 또는 NewPassword)
     * @return 해싱된 비밀번호
     */
    fun hash(password: Password): PasswordHash

    /**
     * 평문 비밀번호와 해시가 일치하는지 검증합니다.
     *
     * @param password 평문 비밀번호
     * @param hash 저장된 해시
     * @return 일치 여부
     */
    fun verify(
        password: Password,
        hash: PasswordHash,
    ): Boolean
}
