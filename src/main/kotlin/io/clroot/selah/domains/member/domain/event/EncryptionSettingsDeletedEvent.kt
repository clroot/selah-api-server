package io.clroot.selah.domains.member.domain.event

import io.clroot.selah.common.event.BaseDomainEvent
import io.clroot.selah.domains.member.domain.MemberId

/**
 * 암호화 설정 삭제 이벤트
 *
 * 이 이벤트가 발행되면 Prayer 도메인에서 해당 회원의
 * 모든 암호화된 데이터를 삭제해야 합니다.
 */
data class EncryptionSettingsDeletedEvent(
    val memberId: MemberId,
) : BaseDomainEvent()
