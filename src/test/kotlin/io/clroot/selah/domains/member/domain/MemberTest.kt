package io.clroot.selah.domains.member.domain

import io.clroot.selah.domains.member.domain.event.*
import io.clroot.selah.domains.member.domain.exception.CannotDisconnectLastLoginMethodException
import io.clroot.selah.domains.member.domain.exception.OAuthProviderAlreadyConnectedException
import io.clroot.selah.domains.member.domain.exception.OAuthProviderNotConnectedException
import io.clroot.selah.domains.member.domain.exception.PasswordNotSetException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MemberTest :
    DescribeSpec({

        describe("Member 생성") {

            context("이메일/비밀번호로 생성할 때") {

                it("유효한 정보로 Member를 생성한다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )

                    member.email.value shouldBe "test@example.com"
                    member.nickname shouldBe "테스트유저"
                    member.passwordHash.shouldNotBeNull()
                    member.emailVerified.shouldBeFalse()
                    member.oauthConnections.shouldBeEmpty()
                    member.role shouldBe Member.Role.USER
                }

                it("MemberRegisteredEvent가 발행된다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<MemberRegisteredEvent>()
                }

                it("프로필 이미지 URL을 설정할 수 있다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("hashed_password"),
                            profileImageUrl = "https://example.com/image.png",
                        )

                    member.profileImageUrl shouldBe "https://example.com/image.png"
                }
            }

            context("OAuth로 생성할 때") {

                it("유효한 정보로 Member를 생성한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    member.email.value shouldBe "test@example.com"
                    member.nickname shouldBe "테스트유저"
                    member.passwordHash.shouldBeNull()
                    member.emailVerified.shouldBeTrue() // OAuth는 이메일 인증됨
                    member.oauthConnections shouldHaveSize 1
                    member.oauthConnections.first().provider shouldBe OAuthProvider.GOOGLE
                    member.role shouldBe Member.Role.USER
                }

                it("MemberRegisteredEvent가 발행된다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<MemberRegisteredEvent>()
                }
            }

            context("불변식 위반") {

                it("닉네임이 빈 문자열이면 실패한다") {
                    shouldThrow<IllegalArgumentException> {
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )
                    }
                }

                it("닉네임이 공백만 있으면 실패한다") {
                    shouldThrow<IllegalArgumentException> {
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "   ",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )
                    }
                }
            }
        }

        describe("OAuth 관리") {

            context("OAuth 연결") {

                it("새로운 OAuth Provider를 연결할 수 있다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )
                    member.clearEvents()

                    member.connectOAuth(OAuthProvider.GOOGLE, "google_123")

                    member.oauthConnections shouldHaveSize 1
                    member.hasProvider(OAuthProvider.GOOGLE).shouldBeTrue()
                }

                it("OAuthConnectedEvent가 발행된다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("hashed_password"),
                        )
                    member.clearEvents()

                    member.connectOAuth(OAuthProvider.GOOGLE, "google_123")

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<OAuthConnectedEvent>()
                }

                it("이미 연결된 Provider를 다시 연결하면 실패한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    shouldThrow<OAuthProviderAlreadyConnectedException> {
                        member.connectOAuth(OAuthProvider.GOOGLE, "google_456")
                    }
                }

                it("여러 Provider를 연결할 수 있다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    member.connectOAuth(OAuthProvider.KAKAO, "kakao_123")
                    member.connectOAuth(OAuthProvider.NAVER, "naver_123")

                    member.oauthConnections shouldHaveSize 3
                    member.connectedProviders shouldContainExactly
                        setOf(
                            OAuthProvider.GOOGLE,
                            OAuthProvider.KAKAO,
                            OAuthProvider.NAVER,
                        )
                }
            }

            context("OAuth 연결 해제") {

                it("비밀번호가 있으면 마지막 OAuth를 해제할 수 있다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )
                    member.setPassword(PasswordHash.from("hashed_password"))
                    member.clearEvents()

                    member.disconnectOAuth(OAuthProvider.GOOGLE)

                    member.oauthConnections.shouldBeEmpty()
                    member.hasProvider(OAuthProvider.GOOGLE).shouldBeFalse()
                }

                it("OAuthDisconnectedEvent가 발행된다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )
                    member.connectOAuth(OAuthProvider.KAKAO, "kakao_123")
                    member.clearEvents()

                    member.disconnectOAuth(OAuthProvider.GOOGLE)

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<OAuthDisconnectedEvent>()
                }

                it("비밀번호 없이 마지막 OAuth를 해제하면 실패한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    shouldThrow<CannotDisconnectLastLoginMethodException> {
                        member.disconnectOAuth(OAuthProvider.GOOGLE)
                    }
                }

                it("연결되지 않은 Provider를 해제하면 실패한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    shouldThrow<OAuthProviderNotConnectedException> {
                        member.disconnectOAuth(OAuthProvider.KAKAO)
                    }
                }

                it("여러 OAuth 중 하나를 해제할 수 있다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )
                    member.connectOAuth(OAuthProvider.KAKAO, "kakao_123")

                    member.disconnectOAuth(OAuthProvider.GOOGLE)

                    member.oauthConnections shouldHaveSize 1
                    member.hasProvider(OAuthProvider.KAKAO).shouldBeTrue()
                    member.hasProvider(OAuthProvider.GOOGLE).shouldBeFalse()
                }
            }

            context("OAuth 조회") {

                it("Provider ID로 연결을 찾을 수 있다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    val connection = member.findConnectionByProviderId(OAuthProvider.GOOGLE, "google_123")

                    connection.shouldNotBeNull()
                    connection.providerId shouldBe "google_123"
                }

                it("없는 Provider ID로 찾으면 null을 반환한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    val connection = member.findConnectionByProviderId(OAuthProvider.GOOGLE, "wrong_id")

                    connection.shouldBeNull()
                }

                it("primaryProvider는 가장 먼저 연결된 Provider를 반환한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    member.primaryProvider shouldBe OAuthProvider.GOOGLE
                }
            }
        }

        describe("비밀번호 관리") {

            context("비밀번호 설정") {

                it("OAuth 사용자가 비밀번호를 설정할 수 있다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )
                    member.clearEvents()

                    member.setPassword(PasswordHash.from("hashed_password"))

                    member.hasPassword.shouldBeTrue()
                    member.passwordHash.shouldNotBeNull()
                }

                it("PasswordSetEvent가 발행된다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )
                    member.clearEvents()

                    member.setPassword(PasswordHash.from("hashed_password"))

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<PasswordSetEvent>()
                }
            }

            context("비밀번호 변경") {

                it("기존 비밀번호를 변경할 수 있다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("old_password"),
                        )
                    member.clearEvents()

                    member.changePassword(PasswordHash.from("new_password"))

                    member.passwordHash?.value shouldBe "new_password"
                }

                it("PasswordChangedEvent가 발행된다") {
                    val member =
                        Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            passwordHash = PasswordHash.from("old_password"),
                        )
                    member.clearEvents()

                    member.changePassword(PasswordHash.from("new_password"))

                    member.domainEvents shouldHaveSize 1
                    member.domainEvents.first().shouldBeInstanceOf<PasswordChangedEvent>()
                }

                it("비밀번호가 없는 사용자가 변경하면 실패한다") {
                    val member =
                        Member.createWithOAuth(
                            email = Email("test@example.com"),
                            nickname = "테스트유저",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google_123",
                        )

                    shouldThrow<PasswordNotSetException> {
                        member.changePassword(PasswordHash.from("new_password"))
                    }
                }
            }
        }

        describe("이메일 인증") {

            it("이메일 인증을 완료할 수 있다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.verifyEmail()

                member.emailVerified.shouldBeTrue()
            }

            it("EmailVerifiedEvent가 발행된다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.verifyEmail()

                member.domainEvents shouldHaveSize 1
                member.domainEvents.first().shouldBeInstanceOf<EmailVerifiedEvent>()
            }

            it("이미 인증된 경우 이벤트가 발행되지 않는다") {
                val member =
                    Member.createWithOAuth(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        provider = OAuthProvider.GOOGLE,
                        providerId = "google_123",
                    )
                member.clearEvents()

                member.verifyEmail()

                member.domainEvents.shouldBeEmpty()
            }
        }

        describe("프로필 업데이트") {

            it("닉네임을 업데이트할 수 있다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.updateProfile(newNickname = "새닉네임")

                member.nickname shouldBe "새닉네임"
            }

            it("프로필 이미지를 업데이트할 수 있다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.updateProfile(newProfileImageUrl = "https://example.com/new.png")

                member.profileImageUrl shouldBe "https://example.com/new.png"
            }

            it("MemberProfileUpdatedEvent가 발행된다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.updateProfile(newNickname = "새닉네임")

                member.domainEvents shouldHaveSize 1
                member.domainEvents.first().shouldBeInstanceOf<MemberProfileUpdatedEvent>()
            }

            it("같은 값으로 업데이트하면 이벤트가 발행되지 않는다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                member.clearEvents()

                member.updateProfile(newNickname = "테스트유저")

                member.domainEvents.shouldBeEmpty()
            }

            it("빈 닉네임으로 업데이트하면 실패한다") {
                val member =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )

                shouldThrow<IllegalArgumentException> {
                    member.updateProfile(newNickname = "")
                }
            }
        }

        describe("Computed Properties") {

            it("hasPassword는 비밀번호 존재 여부를 반환한다") {
                val memberWithPassword =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )
                val memberWithoutPassword =
                    Member.createWithOAuth(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        provider = OAuthProvider.GOOGLE,
                        providerId = "google_123",
                    )

                memberWithPassword.hasPassword.shouldBeTrue()
                memberWithoutPassword.hasPassword.shouldBeFalse()
            }

            it("hasOAuthConnection은 OAuth 연결 존재 여부를 반환한다") {
                val memberWithOAuth =
                    Member.createWithOAuth(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        provider = OAuthProvider.GOOGLE,
                        providerId = "google_123",
                    )
                val memberWithoutOAuth =
                    Member.createWithEmail(
                        email = Email("test@example.com"),
                        nickname = "테스트유저",
                        passwordHash = PasswordHash.from("hashed_password"),
                    )

                memberWithOAuth.hasOAuthConnection.shouldBeTrue()
                memberWithoutOAuth.hasOAuthConnection.shouldBeFalse()
            }
        }
    })
