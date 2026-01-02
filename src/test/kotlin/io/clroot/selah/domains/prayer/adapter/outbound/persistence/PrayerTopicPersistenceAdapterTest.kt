package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@SpringBootTest
class PrayerTopicPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: PrayerTopicPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    init {
        describe("PrayerTopicPersistenceAdapter") {
            lateinit var testMember: Member

            beforeEach {
                testMember = createAndSaveMember()
            }

            describe("save") {
                context("새 기도제목을 저장할 때") {
                    it("저장된 기도제목을 반환한다") {
                        // Given
                        val prayerTopic = PrayerTopic.create(
                            memberId = testMember.id,
                            title = "암호화된 기도제목",
                        )

                        // When
                        val saved = adapter.save(prayerTopic)

                        // Then
                        saved.id shouldBe prayerTopic.id
                        saved.memberId shouldBe testMember.id
                        saved.title shouldBe "암호화된 기도제목"
                        saved.status shouldBe PrayerTopicStatus.PRAYING
                        saved.answeredAt.shouldBeNull()
                        saved.reflection.shouldBeNull()
                    }
                }

                context("기존 기도제목을 수정할 때") {
                    it("업데이트된 기도제목을 반환한다") {
                        // Given
                        val prayerTopic = createAndSavePrayerTopic(testMember.id)
                        prayerTopic.updateTitle("수정된 암호화 제목")

                        // When
                        val updated = adapter.save(prayerTopic)

                        // Then
                        updated.id shouldBe prayerTopic.id
                        updated.title shouldBe "수정된 암호화 제목"
                    }
                }

                context("기도제목을 응답 상태로 변경할 때") {
                    it("응답 상태로 저장된다") {
                        // Given
                        val prayerTopic = createAndSavePrayerTopic(testMember.id)
                        prayerTopic.markAsAnswered("감사 소감")

                        // When
                        val updated = adapter.save(prayerTopic)

                        // Then
                        updated.status shouldBe PrayerTopicStatus.ANSWERED
                        updated.answeredAt.shouldNotBeNull()
                        updated.reflection shouldBe "감사 소감"
                    }
                }
            }

            describe("findById") {
                context("기도제목이 존재할 때") {
                    it("기도제목을 반환한다") {
                        // Given
                        val prayerTopic = createAndSavePrayerTopic(testMember.id)

                        // When
                        val found = adapter.findById(prayerTopic.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe prayerTopic.id
                        found.title shouldBe prayerTopic.title
                    }
                }

                context("기도제목이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val nonExistentId = io.clroot.selah.domains.prayer.domain.PrayerTopicId.new()

                        // When
                        val found = adapter.findById(nonExistentId)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findByIdAndMemberId") {
                context("기도제목이 해당 회원의 것일 때") {
                    it("기도제목을 반환한다") {
                        // Given
                        val prayerTopic = createAndSavePrayerTopic(testMember.id)

                        // When
                        val found = adapter.findByIdAndMemberId(prayerTopic.id, testMember.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe prayerTopic.id
                        found.memberId shouldBe testMember.id
                    }
                }

                context("기도제목이 다른 회원의 것일 때") {
                    it("null을 반환한다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        val prayerTopic = createAndSavePrayerTopic(otherMember.id)

                        // When
                        val found = adapter.findByIdAndMemberId(prayerTopic.id, testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("기도제목이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val nonExistentId = io.clroot.selah.domains.prayer.domain.PrayerTopicId.new()

                        // When
                        val found = adapter.findByIdAndMemberId(nonExistentId, testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findAllByMemberId") {
                context("회원의 기도제목이 여러 개 있을 때") {
                    it("페이징된 기도제목 목록을 반환한다") {
                        // Given
                        repeat(5) {
                            createAndSavePrayerTopic(testMember.id, "기도제목 $it")
                        }
                        val pageable = PageRequest.of(0, 3)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, null, pageable)

                        // Then
                        page.content shouldHaveSize 3
                        page.totalElements shouldBe 5
                        page.totalPages shouldBe 2
                    }
                }

                context("상태 필터가 지정되었을 때") {
                    it("해당 상태의 기도제목만 반환한다") {
                        // Given
                        createAndSavePrayerTopic(testMember.id, "기도중 1")
                        createAndSavePrayerTopic(testMember.id, "기도중 2")
                        val answered = createAndSavePrayerTopic(testMember.id, "응답됨")
                        answered.markAsAnswered()
                        adapter.save(answered)

                        val pageable = PageRequest.of(0, 10)

                        // When
                        val prayingPage = adapter.findAllByMemberId(
                            testMember.id,
                            PrayerTopicStatus.PRAYING,
                            pageable,
                        )
                        val answeredPage = adapter.findAllByMemberId(
                            testMember.id,
                            PrayerTopicStatus.ANSWERED,
                            pageable,
                        )

                        // Then
                        prayingPage.content shouldHaveSize 2
                        prayingPage.content.forEach { it.status shouldBe PrayerTopicStatus.PRAYING }

                        answeredPage.content shouldHaveSize 1
                        answeredPage.content.forEach { it.status shouldBe PrayerTopicStatus.ANSWERED }
                    }
                }

                context("기도제목이 없을 때") {
                    it("빈 페이지를 반환한다") {
                        // Given
                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, null, pageable)

                        // Then
                        page.content.shouldBeEmpty()
                        page.totalElements shouldBe 0
                    }
                }

                context("다른 회원의 기도제목은") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        createAndSavePrayerTopic(otherMember.id)
                        createAndSavePrayerTopic(testMember.id)

                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, null, pageable)

                        // Then
                        page.content shouldHaveSize 1
                        page.content.first().memberId shouldBe testMember.id
                    }
                }
            }

            describe("deleteById") {
                context("기도제목이 존재할 때") {
                    it("삭제된다") {
                        // Given
                        val prayerTopic = createAndSavePrayerTopic(testMember.id)

                        // When
                        adapter.deleteById(prayerTopic.id)

                        // Then
                        val found = adapter.findById(prayerTopic.id)
                        found.shouldBeNull()
                    }
                }

                context("기도제목이 존재하지 않을 때") {
                    it("에러 없이 정상 처리된다") {
                        // Given
                        val nonExistentId = io.clroot.selah.domains.prayer.domain.PrayerTopicId.new()

                        // When & Then - 에러 없이 실행
                        adapter.deleteById(nonExistentId)
                    }
                }
            }

            describe("findCandidatesForLookback") {
                context("cutoff date 이전의 기도제목이 있을 때") {
                    it("후보 목록을 반환한다") {
                        // Given
                        val old1 = createAndSavePrayerTopic(testMember.id, "오래된 1")
                        val old2 = createAndSavePrayerTopic(testMember.id, "오래된 2")
                        Thread.sleep(100)
                        val recent = createAndSavePrayerTopic(testMember.id, "최근")

                        val cutoffDate = recent.createdAt

                        // When
                        val candidates = adapter.findCandidatesForLookback(
                            testMember.id,
                            cutoffDate,
                            emptyList(),
                        )

                        // Then
                        candidates shouldHaveSize 2
                        candidates.map { it.id } shouldContainExactlyInAnyOrder listOf(old1.id, old2.id)
                    }
                }

                context("제외할 ID가 지정되었을 때") {
                    it("제외된 기도제목은 반환하지 않는다") {
                        // Given
                        val topic1 = createAndSavePrayerTopic(testMember.id, "제목 1")
                        val topic2 = createAndSavePrayerTopic(testMember.id, "제목 2")
                        val topic3 = createAndSavePrayerTopic(testMember.id, "제목 3")

                        val cutoffDate = LocalDateTime.now().plusDays(1)

                        // When
                        val candidates = adapter.findCandidatesForLookback(
                            testMember.id,
                            cutoffDate,
                            listOf(topic2.id),
                        )

                        // Then
                        candidates shouldHaveSize 2
                        candidates.map { it.id } shouldContainExactlyInAnyOrder listOf(topic1.id, topic3.id)
                    }
                }

                context("모든 기도제목이 최근인 경우") {
                    it("빈 리스트를 반환한다") {
                        // Given
                        createAndSavePrayerTopic(testMember.id)
                        val cutoffDate = LocalDateTime.now().minusDays(1)

                        // When
                        val candidates = adapter.findCandidatesForLookback(
                            testMember.id,
                            cutoffDate,
                            emptyList(),
                        )

                        // Then
                        candidates.shouldBeEmpty()
                    }
                }

                context("다른 회원의 기도제목은") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        createAndSavePrayerTopic(otherMember.id)
                        createAndSavePrayerTopic(testMember.id)

                        val cutoffDate = LocalDateTime.now().plusDays(1)

                        // When
                        val candidates = adapter.findCandidatesForLookback(
                            testMember.id,
                            cutoffDate,
                            emptyList(),
                        )

                        // Then
                        candidates shouldHaveSize 1
                        candidates.first().memberId shouldBe testMember.id
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

    private suspend fun createAndSavePrayerTopic(
        memberId: MemberId,
        title: String = "암호화된 기도제목-${System.currentTimeMillis()}",
    ): PrayerTopic {
        val topic = PrayerTopic.create(
            memberId = memberId,
            title = title,
        )
        return adapter.save(topic)
    }
}
