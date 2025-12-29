package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.*
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberPersistenceAdapterIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var memberPersistenceAdapter: MemberPersistenceAdapter

    @Autowired
    private lateinit var memberJpaRepository: MemberJpaRepository

    init {
        describe("MemberPersistenceAdapter") {
            afterEach {
                memberJpaRepository.deleteAll()
            }

            describe("save") {
                context("이메일/비밀번호로 생성된 새 Member를 저장할 때") {
                    it("Member가 정상적으로 저장된다") {
                        val member = Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "TestUser",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )

                        val savedMember = memberPersistenceAdapter.save(member)

                        savedMember.id shouldBe member.id
                        savedMember.email shouldBe member.email
                        savedMember.nickname shouldBe member.nickname
                        savedMember.passwordHash shouldBe member.passwordHash
                        savedMember.emailVerified shouldBe false
                        savedMember.role shouldBe Member.Role.USER
                        savedMember.oauthConnections shouldHaveSize 0

                        // DB에서 직접 확인
                        val entity = memberJpaRepository.findById(member.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.email shouldBe member.email.value
                    }
                }

                context("OAuth로 생성된 새 Member를 저장할 때") {
                    it("Member와 OAuthConnection이 함께 저장된다") {
                        val member = Member.createWithOAuth(
                            email = Email("oauth@example.com"),
                            nickname = "OAuthUser",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-id-123",
                            profileImageUrl = "https://example.com/profile.jpg",
                        )

                        val savedMember = memberPersistenceAdapter.save(member)

                        savedMember.id shouldBe member.id
                        savedMember.email shouldBe member.email
                        savedMember.nickname shouldBe member.nickname
                        savedMember.passwordHash.shouldBeNull()
                        savedMember.emailVerified shouldBe true
                        savedMember.oauthConnections shouldHaveSize 1

                        val connection = savedMember.oauthConnections.first()
                        connection.provider shouldBe OAuthProvider.GOOGLE
                        connection.providerId shouldBe "google-id-123"
                    }
                }

                context("기존 Member를 업데이트할 때") {
                    it("변경된 정보가 저장된다") {
                        val member = Member.createWithEmail(
                            email = Email("update@example.com"),
                            nickname = "OriginalNickname",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )

                        val savedMember = memberPersistenceAdapter.save(member)

                        // 프로필 업데이트
                        savedMember.updateProfile(newNickname = "UpdatedNickname")

                        val updatedMember = memberPersistenceAdapter.save(savedMember)

                        updatedMember.nickname shouldBe "UpdatedNickname"

                        // DB에서 확인
                        val entity = memberJpaRepository.findById(member.id.value).orElse(null)
                        entity.shouldNotBeNull()
                        entity.nickname shouldBe "UpdatedNickname"
                    }
                }

                context("OAuth 연결을 추가할 때") {
                    it("새 OAuth 연결이 저장된다") {
                        val member = Member.createWithEmail(
                            email = Email("addingoauth@example.com"),
                            nickname = "EmailUser",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )

                        val savedMember = memberPersistenceAdapter.save(member)
                        savedMember.verifyEmail()
                        savedMember.connectOAuth(OAuthProvider.GOOGLE, "google-id-456")

                        val updatedMember = memberPersistenceAdapter.save(savedMember)

                        updatedMember.oauthConnections shouldHaveSize 1
                        updatedMember.hasProvider(OAuthProvider.GOOGLE) shouldBe true
                    }
                }
            }

            describe("findById") {
                context("존재하는 Member ID로 조회할 때") {
                    it("Member를 반환한다") {
                        val member = Member.createWithEmail(
                            email = Email("findbyid@example.com"),
                            nickname = "FindById",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )
                        memberPersistenceAdapter.save(member)

                        val foundMember = memberPersistenceAdapter.findById(member.id)

                        foundMember.shouldNotBeNull()
                        foundMember.id shouldBe member.id
                        foundMember.email shouldBe member.email
                    }
                }

                context("존재하지 않는 Member ID로 조회할 때") {
                    it("null을 반환한다") {
                        val nonExistentId = MemberId.new()

                        val foundMember = memberPersistenceAdapter.findById(nonExistentId)

                        foundMember.shouldBeNull()
                    }
                }
            }

            describe("findByEmail") {
                context("존재하는 이메일로 조회할 때") {
                    it("Member를 반환한다") {
                        val email = Email("findbyemail@example.com")
                        val member = Member.createWithEmail(
                            email = email,
                            nickname = "FindByEmail",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )
                        memberPersistenceAdapter.save(member)

                        val foundMember = memberPersistenceAdapter.findByEmail(email)

                        foundMember.shouldNotBeNull()
                        foundMember.email shouldBe email
                    }
                }

                context("존재하지 않는 이메일로 조회할 때") {
                    it("null을 반환한다") {
                        val nonExistentEmail = Email("nonexistent@example.com")

                        val foundMember = memberPersistenceAdapter.findByEmail(nonExistentEmail)

                        foundMember.shouldBeNull()
                    }
                }
            }

            describe("findByOAuthConnection") {
                context("존재하는 OAuth 연결로 조회할 때") {
                    it("Member를 반환한다") {
                        val provider = OAuthProvider.GOOGLE
                        val providerId = "google-oauth-find-123"
                        val member = Member.createWithOAuth(
                            email = Email("oauthfind@example.com"),
                            nickname = "OAuthFind",
                            provider = provider,
                            providerId = providerId,
                        )
                        memberPersistenceAdapter.save(member)

                        val foundMember = memberPersistenceAdapter.findByOAuthConnection(provider, providerId)

                        foundMember.shouldNotBeNull()
                        foundMember.hasProvider(provider) shouldBe true
                        foundMember.findConnectionByProviderId(provider, providerId).shouldNotBeNull()
                    }
                }

                context("존재하지 않는 OAuth 연결로 조회할 때") {
                    it("null을 반환한다") {
                        val foundMember = memberPersistenceAdapter.findByOAuthConnection(
                            OAuthProvider.GOOGLE,
                            "nonexistent-provider-id"
                        )

                        foundMember.shouldBeNull()
                    }
                }
            }

            describe("existsByEmail") {
                context("이메일이 존재할 때") {
                    it("true를 반환한다") {
                        val email = Email("exists@example.com")
                        val member = Member.createWithEmail(
                            email = email,
                            nickname = "Exists",
                            passwordHash = PasswordHash.from("\$argon2id\$hashedvalue"),
                        )
                        memberPersistenceAdapter.save(member)

                        val exists = memberPersistenceAdapter.existsByEmail(email)

                        exists shouldBe true
                    }
                }

                context("이메일이 존재하지 않을 때") {
                    it("false를 반환한다") {
                        val nonExistentEmail = Email("notexists@example.com")

                        val exists = memberPersistenceAdapter.existsByEmail(nonExistentEmail)

                        exists shouldBe false
                    }
                }
            }

            describe("복합 시나리오") {
                context("여러 OAuth Provider를 가진 Member") {
                    it("모든 OAuth 연결이 저장되고 조회된다") {
                        val member = Member.createWithOAuth(
                            email = Email("multioauth@example.com"),
                            nickname = "MultiOAuth",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-multi-123",
                        )

                        val savedMember = memberPersistenceAdapter.save(member)

                        // 추가 OAuth 연결
                        savedMember.connectOAuth(OAuthProvider.KAKAO, "kakao-multi-456")

                        val updatedMember = memberPersistenceAdapter.save(savedMember)

                        updatedMember.oauthConnections shouldHaveSize 2
                        updatedMember.connectedProviders shouldContainExactlyInAnyOrder listOf(
                            OAuthProvider.GOOGLE,
                            OAuthProvider.KAKAO,
                        )

                        // 각 Provider로 조회
                        val byGoogle = memberPersistenceAdapter.findByOAuthConnection(
                            OAuthProvider.GOOGLE,
                            "google-multi-123"
                        )
                        byGoogle.shouldNotBeNull()

                        val byKakao = memberPersistenceAdapter.findByOAuthConnection(
                            OAuthProvider.KAKAO,
                            "kakao-multi-456"
                        )
                        byKakao.shouldNotBeNull()
                        byKakao.id shouldBe byGoogle.id
                    }
                }
            }
        }
    }
}
