package io.clroot.selah.domains.member.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class OAuthConnectionTest : DescribeSpec({

    describe("OAuthConnection 생성") {

        it("유효한 정보로 생성한다") {
            val connection = OAuthConnection.create(
                provider = OAuthProvider.GOOGLE,
                providerId = "google_123",
            )

            connection.id.shouldNotBeNull()
            connection.provider shouldBe OAuthProvider.GOOGLE
            connection.providerId shouldBe "google_123"
            connection.connectedAt.shouldNotBeNull()
        }

        it("다양한 Provider로 생성할 수 있다") {
            val googleConnection = OAuthConnection.create(OAuthProvider.GOOGLE, "google_id")
            val kakaoConnection = OAuthConnection.create(OAuthProvider.KAKAO, "kakao_id")
            val naverConnection = OAuthConnection.create(OAuthProvider.NAVER, "naver_id")

            googleConnection.provider shouldBe OAuthProvider.GOOGLE
            kakaoConnection.provider shouldBe OAuthProvider.KAKAO
            naverConnection.provider shouldBe OAuthProvider.NAVER
        }

        it("providerId가 빈 문자열이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                OAuthConnection.create(
                    provider = OAuthProvider.GOOGLE,
                    providerId = "",
                )
            }
        }

        it("providerId가 공백만 있으면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                OAuthConnection.create(
                    provider = OAuthProvider.GOOGLE,
                    providerId = "   ",
                )
            }
        }
    }

    describe("OAuthConnection ID") {

        it("각 연결은 고유한 ID를 갖는다") {
            val connection1 = OAuthConnection.create(OAuthProvider.GOOGLE, "google_1")
            OAuthConnection.create(OAuthProvider.GOOGLE, "google_2")

            connection1.id shouldBe connection1.id // 동일 객체는 같은 ID
            connection1.id.value shouldBe connection1.id.value
        }
    }
})
