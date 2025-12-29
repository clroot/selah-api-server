package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.event.MemberCreatedEvent
import io.clroot.selah.domains.member.domain.event.MemberProfileUpdatedEvent
import java.time.LocalDateTime

class Member(
    id: MemberId,
    // --- 비즈니스 필드 ---
    email: Email,
    nickname: String,
    profileImageUrl: String?,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<MemberId>(id, version, createdAt, updatedAt) {

    var email: Email = email
        private set

    var nickname: String = nickname
        private set

    var profileImageUrl: String? = profileImageUrl
        private set

    fun updateProfile(nickname: String?, profileImageUrl: String?) {
        var isUpdated = false

        nickname?.let {
            if (this.nickname != it) {
                this.nickname = it
                isUpdated = true
            }
        }

        profileImageUrl?.let {
            if (this.profileImageUrl != it) {
                this.profileImageUrl = it
                isUpdated = true
            }
        }

        if (isUpdated) {
            touch()
            registerEvent(MemberProfileUpdatedEvent(this))
        }
    }

    companion object {
        fun create(
            email: Email,
            nickname: String,
            profileImageUrl: String? = null
        ): Member {
            val now = LocalDateTime.now()

            val member = Member(
                id = MemberId.new(),
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
            member.registerEvent(MemberCreatedEvent(member))
            return member
        }
    }
}
