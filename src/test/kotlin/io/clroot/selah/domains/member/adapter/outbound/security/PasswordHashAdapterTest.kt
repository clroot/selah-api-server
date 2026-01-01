package io.clroot.selah.domains.member.adapter.outbound.security

import io.clroot.selah.domains.member.domain.NewPassword
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.member.domain.RawPassword
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

class PasswordHashAdapterTest :
    DescribeSpec({

        val passwordHashAdapter = PasswordHashAdapter()

        describe("hash") {

            context("NewPassword를 해싱할 때") {

                it("Argon2 해시를 반환한다") {
                    val password = NewPassword.from("Test1234!")

                    val hash = passwordHashAdapter.hash(password)

                    hash.value shouldStartWith $$"$argon2"
                }

                it("매번 다른 해시를 생성한다 (salt)") {
                    val password = NewPassword.from("Test1234!")

                    val hash1 = passwordHashAdapter.hash(password)
                    val hash2 = passwordHashAdapter.hash(password)

                    hash1.value shouldNotBe hash2.value
                }
            }

            context("RawPassword를 해싱할 때") {

                it("Argon2 해시를 반환한다") {
                    val password = RawPassword("anypassword")

                    val hash = passwordHashAdapter.hash(password)

                    hash.value shouldStartWith $$"$argon2"
                }
            }
        }

        describe("verify") {

            context("올바른 비밀번호로 검증할 때") {

                it("true를 반환한다") {
                    val password = NewPassword.from("Test1234!")
                    val hash = passwordHashAdapter.hash(password)

                    val rawPassword = RawPassword("Test1234!")
                    val result = passwordHashAdapter.verify(rawPassword, hash)

                    result.shouldBeTrue()
                }
            }

            context("잘못된 비밀번호로 검증할 때") {

                it("false를 반환한다") {
                    val password = NewPassword.from("Test1234!")
                    val hash = passwordHashAdapter.hash(password)

                    val wrongPassword = RawPassword("WrongPassword!")
                    val result = passwordHashAdapter.verify(wrongPassword, hash)

                    result.shouldBeFalse()
                }
            }

            context("다른 해시로 검증할 때") {

                it("false를 반환한다") {
                    val password = RawPassword("Test1234!")
                    val wrongHash = PasswordHash.from($$"$argon2id$v=19$m=16384,t=2,p=1$invalidsalt$invalidhashvalue")

                    val result = passwordHashAdapter.verify(password, wrongHash)

                    result.shouldBeFalse()
                }
            }
        }
    })
