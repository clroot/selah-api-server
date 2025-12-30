package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.domains.member.domain.MemberId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PrayerTopicTest : DescribeSpec({

    describe("PrayerTopic 생성") {

        context("유효한 정보로 생성할 때") {

            it("새 기도제목이 생성된다") {
                val memberId = MemberId.new()
                val title = "encrypted_title_base64"

                val prayerTopic = PrayerTopic.create(
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
                val prayerTopic = PrayerTopic.create(
                    memberId = MemberId.new(),
                    title = "encrypted_title",
                )

                prayerTopic.id.value.length shouldBe 26
            }

            it("createdAt과 updatedAt이 설정된다") {
                val prayerTopic = PrayerTopic.create(
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
                val prayerTopic = PrayerTopic.create(
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
                val prayerTopic = PrayerTopic.create(
                    memberId = MemberId.new(),
                    title = "same_title",
                )
                val originalUpdatedAt = prayerTopic.updatedAt

                prayerTopic.updateTitle("same_title")

                prayerTopic.title shouldBe "same_title"
                prayerTopic.updatedAt shouldBe originalUpdatedAt
            }

            it("빈 title로 수정하면 실패한다") {
                val prayerTopic = PrayerTopic.create(
                    memberId = MemberId.new(),
                    title = "original_title",
                )

                shouldThrow<IllegalArgumentException> {
                    prayerTopic.updateTitle("")
                }
            }

            it("공백만 있는 title로 수정하면 실패한다") {
                val prayerTopic = PrayerTopic.create(
                    memberId = MemberId.new(),
                    title = "original_title",
                )

                shouldThrow<IllegalArgumentException> {
                    prayerTopic.updateTitle("   ")
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
