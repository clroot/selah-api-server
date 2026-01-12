package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicAlreadyAnsweredException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicNotAnsweredException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PrayerTopicTest :
    DescribeSpec({

        describe("PrayerTopic 생성") {

            context("유효한 정보로 생성할 때") {

                it("새 기도제목이 생성된다") {
                    val memberId = MemberId.new()
                    val title = "encrypted_title_base64"

                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = memberId,
                            title = title,
                        )

                    prayerTopic.memberId shouldBe memberId
                    prayerTopic.title shouldBe title
                    prayerTopic.status shouldBe PrayerTopicStatus.PRAYING
                    prayerTopic.answeredAt shouldBe null
                    prayerTopic.reflection shouldBe null
                }

                it("ID가 ULID 형식으로 생성된다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    prayerTopic.id.value.length shouldBe 26
                }

                it("createdAt과 updatedAt이 설정된다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    prayerTopic.createdAt shouldNotBe null
                    prayerTopic.updatedAt shouldNotBe null
                    prayerTopic.createdAt shouldBe prayerTopic.updatedAt
                }
            }

            context("불변식 위반") {

                it("title이 빈 문자열이면 실패한다") {
                    shouldThrow<IllegalArgumentException> {
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "",
                        )
                    }
                }

                it("title이 공백만 있으면 실패한다") {
                    shouldThrow<IllegalArgumentException> {
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "   ",
                        )
                    }
                }
            }
        }

        describe("기도제목 수정") {

            context("updateTitle") {

                it("새로운 title로 수정할 수 있다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "old_title",
                        )
                    val originalUpdatedAt = prayerTopic.updatedAt

                    // 시간 차이를 만들기 위해 잠시 대기
                    Thread.sleep(10)

                    prayerTopic.updateTitle("new_title")

                    prayerTopic.title shouldBe "new_title"
                    prayerTopic.updatedAt shouldNotBe originalUpdatedAt
                }

                it("같은 title로 수정하면 updatedAt이 변경되지 않는다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "same_title",
                        )
                    val originalUpdatedAt = prayerTopic.updatedAt

                    prayerTopic.updateTitle("same_title")

                    prayerTopic.title shouldBe "same_title"
                    prayerTopic.updatedAt shouldBe originalUpdatedAt
                }

                it("빈 title로 수정하면 실패한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "original_title",
                        )

                    shouldThrow<IllegalArgumentException> {
                        prayerTopic.updateTitle("")
                    }
                }

                it("공백만 있는 title로 수정하면 실패한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "original_title",
                        )

                    shouldThrow<IllegalArgumentException> {
                        prayerTopic.updateTitle("   ")
                    }
                }
            }
        }

        describe("응답 체크") {

            context("markAsAnswered") {

                it("PRAYING 상태의 기도제목을 ANSWERED 상태로 변경한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    prayerTopic.markAsAnswered()

                    prayerTopic.status shouldBe PrayerTopicStatus.ANSWERED
                    prayerTopic.answeredAt shouldNotBe null
                }

                it("응답 시 소감을 함께 기록할 수 있다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    val reflection = "encrypted_reflection"

                    prayerTopic.markAsAnswered(reflection)

                    prayerTopic.status shouldBe PrayerTopicStatus.ANSWERED
                    prayerTopic.reflection shouldBe reflection
                }

                it("소감 없이 응답할 수 있다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    prayerTopic.markAsAnswered(null)

                    prayerTopic.status shouldBe PrayerTopicStatus.ANSWERED
                    prayerTopic.reflection shouldBe null
                }

                it("updatedAt이 갱신된다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    val originalUpdatedAt = prayerTopic.updatedAt

                    Thread.sleep(10)
                    prayerTopic.markAsAnswered()

                    prayerTopic.updatedAt shouldNotBe originalUpdatedAt
                }

                it("이미 응답된 기도제목에 대해 호출하면 실패한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered()

                    shouldThrow<PrayerTopicAlreadyAnsweredException> {
                        prayerTopic.markAsAnswered()
                    }
                }
            }

            context("cancelAnswer") {

                it("ANSWERED 상태의 기도제목을 PRAYING 상태로 변경한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered("some_reflection")

                    prayerTopic.cancelAnswer()

                    prayerTopic.status shouldBe PrayerTopicStatus.PRAYING
                    prayerTopic.answeredAt shouldBe null
                    prayerTopic.reflection shouldBe null
                }

                it("updatedAt이 갱신된다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered()
                    val originalUpdatedAt = prayerTopic.updatedAt

                    Thread.sleep(10)
                    prayerTopic.cancelAnswer()

                    prayerTopic.updatedAt shouldNotBe originalUpdatedAt
                }

                it("PRAYING 상태의 기도제목에 대해 호출하면 실패한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    shouldThrow<PrayerTopicNotAnsweredException> {
                        prayerTopic.cancelAnswer()
                    }
                }
            }

            context("updateReflection") {

                it("응답된 기도제목의 소감을 수정할 수 있다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered("old_reflection")

                    prayerTopic.updateReflection("new_reflection")

                    prayerTopic.reflection shouldBe "new_reflection"
                }

                it("소감을 null로 설정하여 삭제할 수 있다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered("some_reflection")

                    prayerTopic.updateReflection(null)

                    prayerTopic.reflection shouldBe null
                }

                it("같은 소감으로 수정하면 updatedAt이 변경되지 않는다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )
                    prayerTopic.markAsAnswered("same_reflection")
                    val originalUpdatedAt = prayerTopic.updatedAt

                    prayerTopic.updateReflection("same_reflection")

                    prayerTopic.updatedAt shouldBe originalUpdatedAt
                }

                it("PRAYING 상태의 기도제목에 대해 호출하면 실패한다") {
                    val prayerTopic =
                        PrayerTopic.create(
                            memberId = MemberId.new(),
                            title = "encrypted_title",
                        )

                    shouldThrow<PrayerTopicNotAnsweredException> {
                        prayerTopic.updateReflection("some_reflection")
                    }
                }
            }
        }

        describe("PrayerTopicId") {

            it("new()로 새 ID를 생성할 수 있다") {
                val id = PrayerTopicId.new()

                id.value.length shouldBe 26
            }

            it("from()으로 기존 값에서 ID를 생성할 수 있다") {
                val originalId = PrayerTopicId.new()
                val recreatedId = PrayerTopicId.from(originalId.value)

                recreatedId shouldBe originalId
            }

            it("유효하지 않은 형식이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    PrayerTopicId.from("invalid-id")
                }
            }
        }
    })
