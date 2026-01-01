package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class PrayerPersistenceAdapterIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var prayerPersistenceAdapter: PrayerPersistenceAdapter

    @Autowired
    private lateinit var prayerJpaRepository: PrayerJpaRepository

    init {
        describe("PrayerPersistenceAdapter") {
            afterEach {
                prayerJpaRepository.deleteAll()
            }

            describe("save") {
                context("새 Prayer를 저장할 때") {
                    it("Prayer가 정상적으로 저장된다") {
                        val memberId = MemberId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = emptyList(),
                            content = "encrypted_content_base64",
                        )

                        val savedPrayer = prayerPersistenceAdapter.save(prayer)

                        savedPrayer.id shouldBe prayer.id
                        savedPrayer.memberId shouldBe memberId
                        savedPrayer.content shouldBe "encrypted_content_base64"
                        savedPrayer.prayerTopicIds shouldHaveSize 0

                        // DB에서 직접 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.content shouldBe "encrypted_content_base64"
                    }
                }

                context("prayerTopicIds가 포함된 Prayer를 저장할 때") {
                    it("prayerTopicIds가 함께 저장된다") {
                        val memberId = MemberId.new()
                        val topicId1 = PrayerTopicId.new()
                        val topicId2 = PrayerTopicId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = listOf(topicId1, topicId2),
                            content = "encrypted_content",
                        )

                        val savedPrayer = prayerPersistenceAdapter.save(prayer)

                        savedPrayer.prayerTopicIds shouldHaveSize 2
                        savedPrayer.prayerTopicIds shouldBe listOf(topicId1, topicId2)

                        // DB에서 직접 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.prayerTopicIds shouldHaveSize 2
                    }
                }

                context("기존 Prayer를 업데이트할 때") {
                    it("변경된 정보가 저장된다") {
                        val memberId = MemberId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = emptyList(),
                            content = "original_content",
                        )

                        val savedPrayer = prayerPersistenceAdapter.save(prayer)

                        // content 업데이트
                        savedPrayer.updateContent("updated_content")

                        val updatedPrayer = prayerPersistenceAdapter.save(savedPrayer)

                        updatedPrayer.content shouldBe "updated_content"

                        // DB에서 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.content shouldBe "updated_content"
                    }
                }

                context("prayerTopicIds를 업데이트할 때") {
                    it("새로운 prayerTopicIds로 교체된다") {
                        val memberId = MemberId.new()
                        val originalTopicId = PrayerTopicId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = listOf(originalTopicId),
                            content = "content",
                        )

                        val savedPrayer = prayerPersistenceAdapter.save(prayer)

                        // prayerTopicIds 업데이트
                        val newTopicId1 = PrayerTopicId.new()
                        val newTopicId2 = PrayerTopicId.new()
                        savedPrayer.updatePrayerTopicIds(listOf(newTopicId1, newTopicId2))

                        val updatedPrayer = prayerPersistenceAdapter.save(savedPrayer)

                        updatedPrayer.prayerTopicIds shouldHaveSize 2
                        updatedPrayer.prayerTopicIds shouldBe listOf(newTopicId1, newTopicId2)

                        // DB에서 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.prayerTopicIds shouldHaveSize 2
                    }
                }

                context("content와 prayerTopicIds를 함께 업데이트할 때") {
                    it("둘 다 변경된다") {
                        val memberId = MemberId.new()
                        val originalTopicId = PrayerTopicId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = listOf(originalTopicId),
                            content = "old_content",
                        )

                        val savedPrayer = prayerPersistenceAdapter.save(prayer)
                        val originalUpdatedAt = savedPrayer.updatedAt

                        // 둘 다 업데이트
                        Thread.sleep(10)
                        val newTopicId = PrayerTopicId.new()
                        savedPrayer.update("new_content", listOf(newTopicId))

                        val updatedPrayer = prayerPersistenceAdapter.save(savedPrayer)

                        updatedPrayer.content shouldBe "new_content"
                        updatedPrayer.prayerTopicIds shouldBe listOf(newTopicId)
                        updatedPrayer.updatedAt shouldNotBe originalUpdatedAt

                        // DB에서 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.content shouldBe "new_content"
                        entity.prayerTopicIds shouldHaveSize 1
                    }
                }
            }

            describe("findById") {
                context("존재하는 Prayer ID로 조회할 때") {
                    it("Prayer를 반환한다") {
                        val memberId = MemberId.new()
                        val topicId = PrayerTopicId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = listOf(topicId),
                            content = "content",
                        )
                        prayerPersistenceAdapter.save(prayer)

                        val foundPrayer = prayerPersistenceAdapter.findById(prayer.id)

                        foundPrayer.shouldNotBeNull()
                        foundPrayer.id shouldBe prayer.id
                        foundPrayer.memberId shouldBe memberId
                        foundPrayer.prayerTopicIds shouldBe listOf(topicId)
                    }
                }

                context("존재하지 않는 Prayer ID로 조회할 때") {
                    it("null을 반환한다") {
                        val nonExistentId = PrayerId.new()

                        val foundPrayer = prayerPersistenceAdapter.findById(nonExistentId)

                        foundPrayer.shouldBeNull()
                    }
                }
            }

            describe("findByIdAndMemberId") {
                context("소유자로 조회할 때") {
                    it("Prayer를 반환한다") {
                        val memberId = MemberId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = emptyList(),
                            content = "content",
                        )
                        prayerPersistenceAdapter.save(prayer)

                        val foundPrayer = prayerPersistenceAdapter.findByIdAndMemberId(prayer.id, memberId)

                        foundPrayer.shouldNotBeNull()
                        foundPrayer.id shouldBe prayer.id
                    }
                }

                context("다른 사용자로 조회할 때") {
                    it("null을 반환한다") {
                        val ownerId = MemberId.new()
                        val otherMemberId = MemberId.new()
                        val prayer = Prayer.create(
                            memberId = ownerId,
                            prayerTopicIds = emptyList(),
                            content = "content",
                        )
                        prayerPersistenceAdapter.save(prayer)

                        val foundPrayer = prayerPersistenceAdapter.findByIdAndMemberId(prayer.id, otherMemberId)

                        foundPrayer.shouldBeNull()
                    }
                }
            }

            describe("findAllByMemberId") {
                context("회원의 기도문이 여러 개 있을 때") {
                    it("페이지네이션하여 반환한다") {
                        val memberId = MemberId.new()
                        repeat(5) { i ->
                            val prayer = Prayer.create(
                                memberId = memberId,
                                prayerTopicIds = emptyList(),
                                content = "content_$i",
                            )
                            prayerPersistenceAdapter.save(prayer)
                        }

                        val pageable = PageRequest.of(0, 3)
                        val result = prayerPersistenceAdapter.findAllByMemberId(memberId, pageable)

                        result.content shouldHaveSize 3
                        result.totalElements shouldBe 5
                        result.totalPages shouldBe 2
                    }
                }

                context("다른 회원의 기도문이 있을 때") {
                    it("요청한 회원의 기도문만 반환한다") {
                        val memberId1 = MemberId.new()
                        val memberId2 = MemberId.new()

                        repeat(3) {
                            prayerPersistenceAdapter.save(
                                Prayer.create(
                                    memberId = memberId1,
                                    prayerTopicIds = emptyList(),
                                    content = "member1_content_$it",
                                ),
                            )
                        }
                        repeat(2) {
                            prayerPersistenceAdapter.save(
                                Prayer.create(
                                    memberId = memberId2,
                                    prayerTopicIds = emptyList(),
                                    content = "member2_content_$it",
                                ),
                            )
                        }

                        val pageable = PageRequest.of(0, 10)
                        val result = prayerPersistenceAdapter.findAllByMemberId(memberId1, pageable)

                        result.content shouldHaveSize 3
                        result.totalElements shouldBe 3
                    }
                }

                context("기도문이 없을 때") {
                    it("빈 페이지를 반환한다") {
                        val memberId = MemberId.new()
                        val pageable = PageRequest.of(0, 10)

                        val result = prayerPersistenceAdapter.findAllByMemberId(memberId, pageable)

                        result.content shouldHaveSize 0
                        result.totalElements shouldBe 0
                    }
                }
            }

            describe("deleteById") {
                context("존재하는 Prayer를 삭제할 때") {
                    it("Prayer가 삭제된다") {
                        val memberId = MemberId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = emptyList(),
                            content = "content",
                        )
                        prayerPersistenceAdapter.save(prayer)

                        prayerPersistenceAdapter.deleteById(prayer.id)

                        val deletedPrayer = prayerPersistenceAdapter.findById(prayer.id)
                        deletedPrayer.shouldBeNull()
                    }
                }

                context("prayerTopicIds가 포함된 Prayer를 삭제할 때") {
                    it("관련 데이터도 함께 삭제된다") {
                        val memberId = MemberId.new()
                        val topicId1 = PrayerTopicId.new()
                        val topicId2 = PrayerTopicId.new()
                        val prayer = Prayer.create(
                            memberId = memberId,
                            prayerTopicIds = listOf(topicId1, topicId2),
                            content = "content",
                        )
                        prayerPersistenceAdapter.save(prayer)

                        prayerPersistenceAdapter.deleteById(prayer.id)

                        val deletedPrayer = prayerPersistenceAdapter.findById(prayer.id)
                        deletedPrayer.shouldBeNull()

                        // DB에서도 삭제 확인
                        val entity = prayerJpaRepository.findById(prayer.id.value).orElse(null)
                        entity.shouldBeNull()
                    }
                }
            }
        }
    }
}
