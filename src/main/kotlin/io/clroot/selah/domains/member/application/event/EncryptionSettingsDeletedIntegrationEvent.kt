package io.clroot.selah.domains.member.application.event

import io.clroot.selah.common.event.BaseIntegrationEvent

/**
 * 암호화 설정 삭제 Integration Event
 *
 * Member Context → Prayer Context로 전파되어
 * 해당 회원의 모든 암호화된 기도 데이터를 삭제합니다.
 *
 * 위치: member/application/event/ (Integration Event)
 * 핸들러 위치: prayer/adapter/inbound/event/
 */
data class EncryptionSettingsDeletedIntegrationEvent(
    val memberId: String,
) : BaseIntegrationEvent()
