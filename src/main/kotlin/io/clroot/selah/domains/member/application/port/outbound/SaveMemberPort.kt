package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Member

/**
 * Member 저장을 위한 Outbound Port
 */
interface SaveMemberPort {
    /**
     * Member를 저장합니다.
     *
     * 신규 저장 및 업데이트 모두 지원합니다.
     *
     * @param member 저장할 Member
     * @return 저장된 Member (version 등 메타데이터 갱신됨)
     */
    suspend fun save(member: Member): Member
}
