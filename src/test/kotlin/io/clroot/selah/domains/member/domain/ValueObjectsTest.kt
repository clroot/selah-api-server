package io.clroot.selah.domains.member.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ValueObjectsTest : DescribeSpec({

    describe("Email") {

        context("유효한 이메일") {

            it("일반적인 이메일 형식을 허용한다") {
                val email = Email("test@example.com")
                email.value shouldBe "test@example.com"
            }

            it("서브도메인이 있는 이메일을 허용한다") {
                val email = Email("test@mail.example.com")
                email.value shouldBe "test@mail.example.com"
            }

            it("특수문자가 포함된 로컬 파트를 허용한다") {
                val email = Email("test.user+tag@example.com")
                email.value shouldBe "test.user+tag@example.com"
            }
        }

        context("유효하지 않은 이메일") {

            it("@ 기호가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("testexample.com")
                }
            }

            it("도메인이 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("test@")
                }
            }

            it("로컬 파트가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("@example.com")
                }
            }

            it("빈 문자열이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("")
                }
            }
        }
    }

    describe("PasswordHash") {

        it("해시된 비밀번호를 저장한다") {
            val hash = PasswordHash.from("hashed_value_123")
            hash.value shouldBe "hashed_value_123"
        }

        it("toString()은 보호된 문자열을 반환한다") {
            val hash = PasswordHash.from("secret_hash")
            hash.toString() shouldBe "[PROTECTED]"
        }

        it("빈 문자열이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                PasswordHash.from("")
            }
        }

        it("공백만 있으면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                PasswordHash.from("   ")
            }
        }
    }

    describe("RawPassword") {

        it("비어있지 않은 비밀번호를 허용한다") {
            val password = RawPassword("any_password")
            password.value shouldBe "any_password"
        }

        it("짧은 비밀번호도 허용한다 (로그인 시도용)") {
            val password = RawPassword("123")
            password.value shouldBe "123"
        }

        it("빈 문자열이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                RawPassword("")
            }
        }
    }

    describe("NewPassword") {

        context("유효한 비밀번호") {

            it("모든 조건을 충족하는 비밀번호를 허용한다") {
                val password = NewPassword.from("Password1!")
                password.value shouldBe "Password1!"
            }

            it("복잡한 비밀번호를 허용한다") {
                val password = NewPassword.from("MyS3cur3P@ssw0rd!")
                password.value shouldBe "MyS3cur3P@ssw0rd!"
            }
        }

        context("유효하지 않은 비밀번호") {

            it("8자 미만이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    NewPassword.from("Pass1!")
                }
            }

            it("영문자가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    NewPassword.from("12345678!")
                }
            }

            it("숫자가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    NewPassword.from("Password!")
                }
            }

            it("특수문자가 없으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    NewPassword.from("Password1")
                }
            }
        }
    }

    describe("MemberId") {

        it("새로운 ID를 생성한다") {
            val id1 = MemberId.new()
            val id2 = MemberId.new()

            id1.value shouldNotBe id2.value
        }

        it("유효한 ULID 문자열로 생성한다") {
            val id = MemberId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
            id.value shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        }

        it("유효하지 않은 형식이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                MemberId.from("invalid-id")
            }
        }
    }

    describe("OAuthConnectionId") {

        it("새로운 ID를 생성한다") {
            val id1 = OAuthConnectionId.new()
            val id2 = OAuthConnectionId.new()

            id1.value shouldNotBe id2.value
        }

        it("유효한 ULID 문자열로 생성한다") {
            val id = OAuthConnectionId.from("01ARZ3NDEKTSV4RRFFQ69G5FAV")
            id.value shouldBe "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        }
    }

    describe("OAuthProvider") {

        it("문자열로 Provider를 찾는다") {
            OAuthProvider.fromString("google") shouldBe OAuthProvider.GOOGLE
            OAuthProvider.fromString("KAKAO") shouldBe OAuthProvider.KAKAO
            OAuthProvider.fromString("Naver") shouldBe OAuthProvider.NAVER
        }

        it("유효하지 않은 문자열이면 실패한다") {
            shouldThrow<IllegalArgumentException> {
                OAuthProvider.fromString("invalid")
            }
        }
    }
})
