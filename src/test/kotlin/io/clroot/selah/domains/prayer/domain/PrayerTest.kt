package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.domains.member.domain.MemberId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PrayerTest : DescribeSpec({

    describe("Prayer 생성") {

        context("유효한 정보로 생성할 때") {

            it("새 기도문이 생성된다") {
                val memberId = MemberId.new()
                val content = "encrypted_content_base64"

                val prayer = Prayer.create(
                    memberId = memberId,
                    content = content,
                )

                prayer.memberId shouldBe memberId
                prayer.content shouldBe content
            }

            it("ID가 ULID 형식으로 생성된다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "encrypted_content",
                )

                prayer.id.value.length shouldBe 26
            }

            it("createdAt과 updatedAt이 설정된다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "encrypted_content",
                )

                prayer.createdAt shouldNotBe null
                prayer.updatedAt shouldNotBe null
                prayer.createdAt shouldBe prayer.updatedAt
            }
        }

        context("불변식 위반") {

            it("content가 빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Prayer.create(
                        memberId = MemberId.new(),
                        content = "",
                    )
                }
            }

            it("content가 공백만 있으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Prayer.create(
                        memberId = MemberId.new(),
                        content = "   ",
                    )
                }
            }
        }
    }

    describe("기도문 수정") {

        context("updateContent") {

            it("새로운 content로 수정할 수 있다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "old_content",
                )
                val originalUpdatedAt = prayer.updatedAt

                // 시간 차이를 만들기 위해 잠시 대기
                Thread.sleep(10)

                prayer.updateContent("new_content")

                prayer.content shouldBe "new_content"
                prayer.updatedAt shouldNotBe originalUpdatedAt
            }

            it("같은 content로 수정하면 updatedAt이 변경되지 않는다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "same_content",
                )
                val originalUpdatedAt = prayer.updatedAt

                prayer.updateContent("same_content")

                prayer.content shouldBe "same_content"
                prayer.updatedAt shouldBe originalUpdatedAt
            }

            it("빈 content로 수정하면 실패한다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "original_content",
                )

                shouldThrow<IllegalArgumentException> {
                    prayer.updateContent("")
                }
            }

            it("공백만 있는 content로 수정하면 실패한다") {
                val prayer = Prayer.create(
                    memberId = MemberId.new(),
                    content = "original_content",
                )

                shouldThrow<IllegalArgumentException> {
                    prayer.updateContent("   ")
                }
            }
        }
    }

    describe("PrayerId") {

        it("new()로 새 ID를 생성할 수 있다") {
            val id = PrayerId.new()

            id.value.length shouldBe 26
        }

        it("from()으로 기존 값에서 ID를 생성할 수 있다") {
            val originalId = PrayerId.new()
            val recreatedId = PrayerId.from(originalId.value)

            recreatedId shouldBe originalId
        }

        it("유효하지 않은 형식이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                PrayerId.from("invalid-id")
            }
        }
    }
})
