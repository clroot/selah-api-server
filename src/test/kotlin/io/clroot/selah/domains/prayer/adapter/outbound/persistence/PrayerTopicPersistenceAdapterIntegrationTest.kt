package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
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
class PrayerTopicPersistenceAdapterIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var prayerTopicPersistenceAdapter: PrayerTopicPersistenceAdapter

    @Autowired
    private lateinit var prayerTopicJpaRepository: PrayerTopicJpaRepository

    init {
        describe("PrayerTopicPersistenceAdapter") {
            afterEach {
                prayerTopicJpaRepository.deleteAll()
            }

            describe("save") {
                context("새 PrayerTopic을 저장할 때") {
                    it("PrayerTopic이 정상적으로 저장된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "encrypted_title_base64",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)

                        savedTopic.id shouldBe prayerTopic.id
                        savedTopic.memberId shouldBe memberId
                        savedTopic.title shouldBe "encrypted_title_base64"
                        savedTopic.status shouldBe PrayerTopicStatus.PRAYING
                        savedTopic.answeredAt.shouldBeNull()
                        savedTopic.reflection.shouldBeNull()

                        // DB에서 직접 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.title shouldBe "encrypted_title_base64"
                        entity.status shouldBe PrayerTopicStatus.PRAYING
                    }
                }

                context("기존 PrayerTopic의 제목을 업데이트할 때") {
                    it("변경된 제목이 저장된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "original_title",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)

                        savedTopic.updateTitle("updated_title")

                        val updatedTopic = prayerTopicPersistenceAdapter.save(savedTopic)

                        updatedTopic.title shouldBe "updated_title"

                        // DB에서 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.title shouldBe "updated_title"
                    }
                }

                context("응답 상태로 변경할 때") {
                    it("status, answeredAt이 업데이트된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)

                        savedTopic.markAsAnswered()

                        val answeredTopic = prayerTopicPersistenceAdapter.save(savedTopic)

                        answeredTopic.status shouldBe PrayerTopicStatus.ANSWERED
                        answeredTopic.answeredAt.shouldNotBeNull()
                        answeredTopic.reflection.shouldBeNull()

                        // DB에서 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.status shouldBe PrayerTopicStatus.ANSWERED
                        entity.answeredAt.shouldNotBeNull()
                    }
                }

                context("응답 상태로 변경하면서 소감을 추가할 때") {
                    it("status, answeredAt, reflection이 모두 저장된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)

                        savedTopic.markAsAnswered("encrypted_reflection")

                        val answeredTopic = prayerTopicPersistenceAdapter.save(savedTopic)

                        answeredTopic.status shouldBe PrayerTopicStatus.ANSWERED
                        answeredTopic.answeredAt.shouldNotBeNull()
                        answeredTopic.reflection shouldBe "encrypted_reflection"

                        // DB에서 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.reflection shouldBe "encrypted_reflection"
                    }
                }

                context("응답 상태를 취소할 때") {
                    it("PRAYING 상태로 되돌아가고 reflection이 삭제된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)
                        savedTopic.markAsAnswered("reflection")
                        val answeredTopic = prayerTopicPersistenceAdapter.save(savedTopic)

                        answeredTopic.cancelAnswer()

                        val cancelledTopic = prayerTopicPersistenceAdapter.save(answeredTopic)

                        cancelledTopic.status shouldBe PrayerTopicStatus.PRAYING
                        cancelledTopic.answeredAt.shouldBeNull()
                        cancelledTopic.reflection.shouldBeNull()

                        // DB에서 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.status shouldBe PrayerTopicStatus.PRAYING
                        entity.answeredAt.shouldBeNull()
                        entity.reflection.shouldBeNull()
                    }
                }

                context("소감을 수정할 때") {
                    it("새 소감으로 업데이트된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )

                        val savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)
                        savedTopic.markAsAnswered("original_reflection")
                        val answeredTopic = prayerTopicPersistenceAdapter.save(savedTopic)

                        answeredTopic.updateReflection("updated_reflection")

                        val updatedTopic = prayerTopicPersistenceAdapter.save(answeredTopic)

                        updatedTopic.reflection shouldBe "updated_reflection"

                        // DB에서 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.reflection shouldBe "updated_reflection"
                    }
                }
            }

            describe("findById") {
                context("존재하는 PrayerTopic ID로 조회할 때") {
                    it("PrayerTopic을 반환한다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )
                        prayerTopicPersistenceAdapter.save(prayerTopic)

                        val foundTopic = prayerTopicPersistenceAdapter.findById(prayerTopic.id)

                        foundTopic.shouldNotBeNull()
                        foundTopic.id shouldBe prayerTopic.id
                        foundTopic.memberId shouldBe memberId
                    }
                }

                context("존재하지 않는 PrayerTopic ID로 조회할 때") {
                    it("null을 반환한다") {
                        val nonExistentId = PrayerTopicId.new()

                        val foundTopic = prayerTopicPersistenceAdapter.findById(nonExistentId)

                        foundTopic.shouldBeNull()
                    }
                }
            }

            describe("findByIdAndMemberId") {
                context("소유자로 조회할 때") {
                    it("PrayerTopic을 반환한다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )
                        prayerTopicPersistenceAdapter.save(prayerTopic)

                        val foundTopic = prayerTopicPersistenceAdapter.findByIdAndMemberId(
                            prayerTopic.id,
                            memberId,
                        )

                        foundTopic.shouldNotBeNull()
                        foundTopic.id shouldBe prayerTopic.id
                    }
                }

                context("다른 사용자로 조회할 때") {
                    it("null을 반환한다") {
                        val ownerId = MemberId.new()
                        val otherMemberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = ownerId,
                            title = "title",
                        )
                        prayerTopicPersistenceAdapter.save(prayerTopic)

                        val foundTopic = prayerTopicPersistenceAdapter.findByIdAndMemberId(
                            prayerTopic.id,
                            otherMemberId,
                        )

                        foundTopic.shouldBeNull()
                    }
                }
            }

            describe("findAllByMemberId") {
                context("상태 필터 없이 조회할 때") {
                    it("해당 회원의 모든 기도제목을 반환한다") {
                        val memberId = MemberId.new()
                        repeat(5) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "title_$it",
                            )
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        val pageable = PageRequest.of(0, 10)
                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId,
                            status = null,
                            pageable,
                        )

                        result.content shouldHaveSize 5
                        result.totalElements shouldBe 5
                    }
                }

                context("PRAYING 상태로 필터링할 때") {
                    it("기도 중인 기도제목만 반환한다") {
                        val memberId = MemberId.new()

                        // PRAYING 상태 3개
                        repeat(3) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "praying_$it",
                            )
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        // ANSWERED 상태 2개
                        repeat(2) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "answered_$it",
                            )
                            topic.markAsAnswered()
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        val pageable = PageRequest.of(0, 10)
                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId,
                            status = PrayerTopicStatus.PRAYING,
                            pageable,
                        )

                        result.content shouldHaveSize 3
                        result.content.all { it.status == PrayerTopicStatus.PRAYING } shouldBe true
                    }
                }

                context("ANSWERED 상태로 필터링할 때") {
                    it("응답된 기도제목만 반환한다") {
                        val memberId = MemberId.new()

                        // PRAYING 상태 2개
                        repeat(2) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "praying_$it",
                            )
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        // ANSWERED 상태 3개
                        repeat(3) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "answered_$it",
                            )
                            topic.markAsAnswered()
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        val pageable = PageRequest.of(0, 10)
                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId,
                            status = PrayerTopicStatus.ANSWERED,
                            pageable,
                        )

                        result.content shouldHaveSize 3
                        result.content.all { it.status == PrayerTopicStatus.ANSWERED } shouldBe true
                    }
                }

                context("페이지네이션할 때") {
                    it("요청된 페이지 크기만큼 반환한다") {
                        val memberId = MemberId.new()
                        repeat(7) {
                            val topic = PrayerTopic.create(
                                memberId = memberId,
                                title = "title_$it",
                            )
                            prayerTopicPersistenceAdapter.save(topic)
                        }

                        val pageable = PageRequest.of(0, 3)
                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId,
                            status = null,
                            pageable,
                        )

                        result.content shouldHaveSize 3
                        result.totalElements shouldBe 7
                        result.totalPages shouldBe 3
                    }
                }

                context("다른 회원의 기도제목이 있을 때") {
                    it("요청한 회원의 기도제목만 반환한다") {
                        val memberId1 = MemberId.new()
                        val memberId2 = MemberId.new()

                        repeat(3) {
                            prayerTopicPersistenceAdapter.save(
                                PrayerTopic.create(
                                    memberId = memberId1,
                                    title = "member1_title_$it",
                                ),
                            )
                        }
                        repeat(2) {
                            prayerTopicPersistenceAdapter.save(
                                PrayerTopic.create(
                                    memberId = memberId2,
                                    title = "member2_title_$it",
                                ),
                            )
                        }

                        val pageable = PageRequest.of(0, 10)
                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId1,
                            status = null,
                            pageable,
                        )

                        result.content shouldHaveSize 3
                        result.totalElements shouldBe 3
                    }
                }

                context("기도제목이 없을 때") {
                    it("빈 페이지를 반환한다") {
                        val memberId = MemberId.new()
                        val pageable = PageRequest.of(0, 10)

                        val result = prayerTopicPersistenceAdapter.findAllByMemberId(
                            memberId,
                            status = null,
                            pageable,
                        )

                        result.content shouldHaveSize 0
                        result.totalElements shouldBe 0
                    }
                }
            }

            describe("deleteById") {
                context("존재하는 PrayerTopic을 삭제할 때") {
                    it("PrayerTopic이 삭제된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )
                        prayerTopicPersistenceAdapter.save(prayerTopic)

                        prayerTopicPersistenceAdapter.deleteById(prayerTopic.id)

                        val deletedTopic = prayerTopicPersistenceAdapter.findById(prayerTopic.id)
                        deletedTopic.shouldBeNull()
                    }
                }

                context("응답된 기도제목을 삭제할 때") {
                    it("관련 데이터도 함께 삭제된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "title",
                        )
                        prayerTopic.markAsAnswered("reflection")
                        prayerTopicPersistenceAdapter.save(prayerTopic)

                        prayerTopicPersistenceAdapter.deleteById(prayerTopic.id)

                        val deletedTopic = prayerTopicPersistenceAdapter.findById(prayerTopic.id)
                        deletedTopic.shouldBeNull()

                        // DB에서도 삭제 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldBeNull()
                    }
                }
            }

            describe("복합 시나리오") {
                context("기도제목 생성 후 응답 체크하고 소감 수정") {
                    it("모든 변경사항이 DB에 반영된다") {
                        val memberId = MemberId.new()
                        val prayerTopic = PrayerTopic.create(
                            memberId = memberId,
                            title = "initial_title",
                        )

                        // 저장
                        var savedTopic = prayerTopicPersistenceAdapter.save(prayerTopic)
                        savedTopic.status shouldBe PrayerTopicStatus.PRAYING

                        // 제목 수정
                        savedTopic.updateTitle("updated_title")
                        savedTopic = prayerTopicPersistenceAdapter.save(savedTopic)
                        savedTopic.title shouldBe "updated_title"

                        // 응답 체크
                        savedTopic.markAsAnswered("first_reflection")
                        savedTopic = prayerTopicPersistenceAdapter.save(savedTopic)
                        savedTopic.status shouldBe PrayerTopicStatus.ANSWERED
                        savedTopic.reflection shouldBe "first_reflection"

                        // 소감 수정
                        savedTopic.updateReflection("updated_reflection")
                        savedTopic = prayerTopicPersistenceAdapter.save(savedTopic)
                        savedTopic.reflection shouldBe "updated_reflection"

                        // DB에서 최종 상태 확인
                        val entity = prayerTopicJpaRepository.findById(prayerTopic.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.title shouldBe "updated_title"
                        entity.status shouldBe PrayerTopicStatus.ANSWERED
                        entity.reflection shouldBe "updated_reflection"
                        entity.answeredAt.shouldNotBeNull()
                    }
                }
            }
        }
    }
}
