package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.adapter.outbound.persistence.member.MemberPersistenceAdapter
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class PrayerPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: PrayerPersistenceAdapter

    @Autowired
    private lateinit var memberAdapter: MemberPersistenceAdapter

    @Autowired
    private lateinit var prayerTopicAdapter: PrayerTopicPersistenceAdapter

    init {
        describe("PrayerPersistenceAdapter") {
            lateinit var testMember: Member
            lateinit var prayerTopic1: PrayerTopic
            lateinit var prayerTopic2: PrayerTopic

            beforeEach {
                testMember = createAndSaveMember()
                prayerTopic1 = createAndSavePrayerTopic(testMember.id, "기도제목 1")
                prayerTopic2 = createAndSavePrayerTopic(testMember.id, "기도제목 2")
            }

            describe("save") {
                context("새 기도문을 저장할 때") {
                    it("저장된 기도문을 반환한다") {
                        // Given
                        val prayer = Prayer.create(
                            memberId = testMember.id,
                            prayerTopicIds = listOf(prayerTopic1.id),
                            content = "암호화된 기도문 내용",
                        )

                        // When
                        val saved = adapter.save(prayer)

                        // Then
                        saved.id shouldBe prayer.id
                        saved.memberId shouldBe testMember.id
                        saved.content shouldBe "암호화된 기도문 내용"
                        saved.prayerTopicIds shouldBe listOf(prayerTopic1.id)
                    }
                }

                context("여러 기도제목과 연결된 기도문을 저장할 때") {
                    it("모든 연결이 저장된다") {
                        // Given
                        val prayer = Prayer.create(
                            memberId = testMember.id,
                            prayerTopicIds = listOf(prayerTopic1.id, prayerTopic2.id),
                            content = "여러 제목의 기도문",
                        )

                        // When
                        val saved = adapter.save(prayer)

                        // Then
                        saved.prayerTopicIds shouldHaveSize 2
                        saved.prayerTopicIds shouldContainExactlyInAnyOrder listOf(
                            prayerTopic1.id,
                            prayerTopic2.id,
                        )
                    }
                }

                context("기존 기도문을 수정할 때") {
                    it("업데이트된 기도문을 반환한다") {
                        // Given
                        val prayer = createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))
                        prayer.updateContent("수정된 내용")

                        // When
                        val updated = adapter.save(prayer)

                        // Then
                        updated.id shouldBe prayer.id
                        updated.content shouldBe "수정된 내용"
                    }
                }

                context("기도문의 연결된 기도제목을 변경할 때") {
                    it("변경사항이 저장된다") {
                        // Given
                        val prayer = createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))
                        prayer.updatePrayerTopicIds(listOf(prayerTopic2.id))

                        // When
                        val updated = adapter.save(prayer)

                        // Then
                        updated.prayerTopicIds shouldBe listOf(prayerTopic2.id)
                    }
                }
            }

            describe("findById") {
                context("기도문이 존재할 때") {
                    it("기도문을 반환한다") {
                        // Given
                        val prayer = createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        // When
                        val found = adapter.findById(prayer.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe prayer.id
                        found.content shouldBe prayer.content
                    }
                }

                context("기도문이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val nonExistentId = PrayerId.new()

                        // When
                        val found = adapter.findById(nonExistentId)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findByIdAndMemberId") {
                context("기도문이 해당 회원의 것일 때") {
                    it("기도문을 반환한다") {
                        // Given
                        val prayer = createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        // When
                        val found = adapter.findByIdAndMemberId(prayer.id, testMember.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe prayer.id
                        found.memberId shouldBe testMember.id
                    }
                }

                context("기도문이 다른 회원의 것일 때") {
                    it("null을 반환한다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        val otherTopic = createAndSavePrayerTopic(otherMember.id)
                        val prayer = createAndSavePrayer(otherMember.id, listOf(otherTopic.id))

                        // When
                        val found = adapter.findByIdAndMemberId(prayer.id, testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }

                context("기도문이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val nonExistentId = PrayerId.new()

                        // When
                        val found = adapter.findByIdAndMemberId(nonExistentId, testMember.id)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findAllByMemberId") {
                context("회원의 기도문이 여러 개 있을 때") {
                    it("페이징된 기도문 목록을 반환한다") {
                        // Given
                        repeat(5) {
                            createAndSavePrayer(testMember.id, listOf(prayerTopic1.id), "기도문 $it")
                        }
                        val pageable = PageRequest.of(0, 3)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, pageable)

                        // Then
                        page.content shouldHaveSize 3
                        page.totalElements shouldBe 5
                        page.totalPages shouldBe 2
                    }
                }

                context("기도문이 없을 때") {
                    it("빈 페이지를 반환한다") {
                        // Given
                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, pageable)

                        // Then
                        page.content.shouldBeEmpty()
                        page.totalElements shouldBe 0
                    }
                }

                context("다른 회원의 기도문은") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        val otherTopic = createAndSavePrayerTopic(otherMember.id)
                        createAndSavePrayer(otherMember.id, listOf(otherTopic.id))
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberId(testMember.id, pageable)

                        // Then
                        page.content shouldHaveSize 1
                        page.content.first().memberId shouldBe testMember.id
                    }
                }
            }

            describe("deleteById") {
                context("기도문이 존재할 때") {
                    it("삭제된다") {
                        // Given
                        val prayer = createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        // When
                        adapter.deleteById(prayer.id)

                        // Then
                        val found = adapter.findById(prayer.id)
                        found.shouldBeNull()
                    }
                }

                context("기도문이 존재하지 않을 때") {
                    it("에러 없이 정상 처리된다") {
                        // Given
                        val nonExistentId = PrayerId.new()

                        // When & Then
                        adapter.deleteById(nonExistentId)
                    }
                }
            }

            describe("findAllByMemberIdAndPrayerTopicId") {
                context("특정 기도제목과 연결된 기도문이 있을 때") {
                    it("해당 기도문만 반환한다") {
                        // Given
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id), "제목1 전용")
                        createAndSavePrayer(testMember.id, listOf(prayerTopic2.id), "제목2 전용")
                        createAndSavePrayer(
                            testMember.id,
                            listOf(prayerTopic1.id, prayerTopic2.id),
                            "둘 다",
                        )

                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page1 = adapter.findAllByMemberIdAndPrayerTopicId(
                            testMember.id,
                            prayerTopic1.id,
                            pageable,
                        )
                        val page2 = adapter.findAllByMemberIdAndPrayerTopicId(
                            testMember.id,
                            prayerTopic2.id,
                            pageable,
                        )

                        // Then
                        page1.content shouldHaveSize 2
                        page2.content shouldHaveSize 2
                    }
                }

                context("연결된 기도문이 없을 때") {
                    it("빈 페이지를 반환한다") {
                        // Given
                        val emptyTopic = createAndSavePrayerTopic(testMember.id, "비어있는 제목")
                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberIdAndPrayerTopicId(
                            testMember.id,
                            emptyTopic.id,
                            pageable,
                        )

                        // Then
                        page.content.shouldBeEmpty()
                    }
                }

                context("다른 회원의 기도문은") {
                    it("조회되지 않는다") {
                        // Given
                        val otherMember = createAndSaveMember()
                        val otherTopic = createAndSavePrayerTopic(otherMember.id)
                        createAndSavePrayer(otherMember.id, listOf(otherTopic.id))
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        val pageable = PageRequest.of(0, 10)

                        // When
                        val page = adapter.findAllByMemberIdAndPrayerTopicId(
                            testMember.id,
                            prayerTopic1.id,
                            pageable,
                        )

                        // Then
                        page.content shouldHaveSize 1
                        page.content.first().memberId shouldBe testMember.id
                    }
                }
            }

            describe("countByPrayerTopicIds") {
                context("여러 기도제목의 기도문 개수를 조회할 때") {
                    it("각 기도제목별 개수 맵을 반환한다") {
                        // Given
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))
                        createAndSavePrayer(testMember.id, listOf(prayerTopic2.id))
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id, prayerTopic2.id))

                        // When
                        val counts = adapter.countByPrayerTopicIds(
                            listOf(prayerTopic1.id, prayerTopic2.id),
                        )

                        // Then
                        counts shouldContainExactly mapOf(
                            prayerTopic1.id to 3L,
                            prayerTopic2.id to 2L,
                        )
                    }
                }

                context("기도문이 없는 기도제목은") {
                    it("맵에 포함되지 않는다") {
                        // Given
                        val emptyTopic = createAndSavePrayerTopic(testMember.id)
                        createAndSavePrayer(testMember.id, listOf(prayerTopic1.id))

                        // When
                        val counts = adapter.countByPrayerTopicIds(
                            listOf(prayerTopic1.id, emptyTopic.id),
                        )

                        // Then
                        counts shouldContainExactly mapOf(prayerTopic1.id to 1L)
                    }
                }

                context("빈 리스트를 전달하면") {
                    it("빈 맵을 반환한다") {
                        // When
                        val counts = adapter.countByPrayerTopicIds(emptyList())

                        // Then
                        counts shouldBe emptyMap()
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
        return prayerTopicAdapter.save(topic)
    }

    private suspend fun createAndSavePrayer(
        memberId: MemberId,
        prayerTopicIds: List<PrayerTopicId>,
        content: String = "암호화된 기도문-${System.currentTimeMillis()}",
    ): Prayer {
        val prayer = Prayer.create(
            memberId = memberId,
            prayerTopicIds = prayerTopicIds,
            content = content,
        )
        return adapter.save(prayer)
    }
}
