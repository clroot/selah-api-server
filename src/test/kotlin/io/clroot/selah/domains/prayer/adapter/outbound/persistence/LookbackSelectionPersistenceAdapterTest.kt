package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.LookbackSelectionId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class LookbackSelectionPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: LookbackSelectionPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    @Autowired
    private lateinit var prayerTopicAdapter: PrayerTopicPersistenceAdapter

    @Autowired
    private lateinit var lookbackSelectionJpaRepository: LookbackSelectionJpaRepository

    @Autowired
    private lateinit var lookbackSelectionMapper: LookbackSelectionMapper

    init {
        describe("LookbackSelectionPersistenceAdapter") {
            lateinit var testMember: Member
            lateinit var prayerTopic: PrayerTopic

            beforeEach {
                testMember = createAndSaveMember()
                prayerTopic = createAndSavePrayerTopic(testMember.id)
            }

            describe("save") {
                context("새 돌아보기 선정을 저장할 때") {
                    it("저장된 선정을 반환한다") {
                        // Given
                        val selection = LookbackSelection.create(
                            memberId = testMember.id,
                            prayerTopicId = prayerTopic.id,
                        )

                        // When
                        val saved = adapter.save(selection)

                        // Then
                        saved.id shouldBe selection.id
                        saved.memberId shouldBe testMember.id
                        saved.prayerTopicId shouldBe prayerTopic.id
                        saved.selectedAt shouldBe LocalDate.now()
                    }
                }
            }

            describe("findByMemberIdAndDate") {
                context("선정이 존재할 때") {
                    it("선정을 반환한다") {
                        // Given
                        val selection = createAndSaveSelection(testMember.id, prayerTopic.id)
                        val today = LocalDate.now()

                        // When
                        val found = adapter.findByMemberIdAndDate(testMember.id, today)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe selection.id
                        found.prayerTopicId shouldBe prayerTopic.id
                    }
                }

                context("선정이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val today = LocalDate.now()

                        // When
                        val found = adapter.findByMemberIdAndDate(testMember.id, today)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("다른 날짜의 선정이 있을 때") {
                    it("해당 날짜의 선정만 반환한다") {
                        // Given
                        createAndSaveSelection(testMember.id, prayerTopic.id)
                        val yesterday = LocalDate.now().minusDays(1)

                        // When
                        val found = adapter.findByMemberIdAndDate(testMember.id, yesterday)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findRecentPrayerTopicIds") {
                context("최근 N일 이내에 선정된 기도제목이 있을 때") {
                    it("해당 기도제목 ID 목록을 반환한다") {
                        // Given - 서로 다른 날짜에 selection 생성 (unique constraint 때문)
                        val topic1 = createAndSavePrayerTopic(testMember.id)
                        val topic2 = createAndSavePrayerTopic(testMember.id)

                        // 오늘과 어제 선정
                        createAndSaveSelectionWithDate(testMember.id, topic1.id, LocalDate.now())
                        createAndSaveSelectionWithDate(testMember.id, topic2.id, LocalDate.now().minusDays(1))

                        // When
                        val recentIds = adapter.findRecentPrayerTopicIds(testMember.id, days = 7)

                        // Then
                        recentIds shouldHaveSize 2
                        recentIds shouldContainExactlyInAnyOrder listOf(topic1.id, topic2.id)
                    }
                }

                context("최근 N일 이내에 선정이 없을 때") {
                    it("빈 리스트를 반환한다") {
                        // When
                        val recentIds = adapter.findRecentPrayerTopicIds(testMember.id, days = 7)

                        // Then
                        recentIds.shouldBeEmpty()
                    }
                }

                context("N일보다 오래된 선정만 있을 때") {
                    it("빈 리스트를 반환한다") {
                        // Given - 10일 전 선정 생성
                        createAndSaveSelectionWithDate(
                            testMember.id,
                            prayerTopic.id,
                            LocalDate.now().minusDays(10),
                        )

                        // When - 최근 7일 조회
                        val recentIds = adapter.findRecentPrayerTopicIds(testMember.id, days = 7)

                        // Then - 10일 전 선정은 7일 범위에 포함되지 않음
                        recentIds.shouldBeEmpty()
                    }
                }
            }

            describe("deleteByMemberIdAndDate - 트랜잭션 테스트") {
                context("선정이 존재할 때") {
                    it("정상적으로 삭제되고 트랜잭션이 커밋된다") {
                        // Given
                        createAndSaveSelection(testMember.id, prayerTopic.id)
                        val today = LocalDate.now()

                        val foundBefore = adapter.findByMemberIdAndDate(testMember.id, today)
                        foundBefore.shouldNotBeNull()

                        // When
                        adapter.deleteByMemberIdAndDate(testMember.id, today)

                        // Then - 실제로 삭제되었는지 확인 (트랜잭션 커밋 확인)
                        val foundAfter = adapter.findByMemberIdAndDate(testMember.id, today)
                        foundAfter.shouldBeNull()
                    }
                }

                context("선정이 존재하지 않을 때") {
                    it("에러 없이 정상 처리된다") {
                        // Given
                        val today = LocalDate.now()

                        // When & Then - 에러 없이 실행
                        adapter.deleteByMemberIdAndDate(testMember.id, today)
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member = Member.createWithEmail(
            email = Email("test-${System.currentTimeMillis()}@example.com"),
            nickname = "테스트 사용자",
            passwordHash = PasswordHash("hashed-password"),
        )
        return memberAdapter.save(member)
    }

    private suspend fun createAndSavePrayerTopic(memberId: MemberId): PrayerTopic {
        val topic = PrayerTopic.create(
            memberId = memberId,
            title = "암호화된 기도제목-${System.currentTimeMillis()}",
        )
        return prayerTopicAdapter.save(topic)
    }

    private suspend fun createAndSaveSelection(
        memberId: MemberId,
        prayerTopicId: PrayerTopicId,
    ): LookbackSelection {
        val selection = LookbackSelection.create(
            memberId = memberId,
            prayerTopicId = prayerTopicId,
        )
        return adapter.save(selection)
    }

    private fun createAndSaveSelectionWithDate(
        memberId: MemberId,
        prayerTopicId: PrayerTopicId,
        selectedAt: LocalDate,
    ): LookbackSelection {
        val now = LocalDateTime.now()
        val entity = LookbackSelectionEntity(
            id = LookbackSelectionId.new().value,
            memberId = memberId.value,
            prayerTopicId = prayerTopicId.value,
            selectedAt = selectedAt,
            version = null,
            createdAt = now,
            updatedAt = now,
        )
        val saved = lookbackSelectionJpaRepository.saveAndFlush(entity)
        return lookbackSelectionMapper.toDomain(saved)
    }
}
