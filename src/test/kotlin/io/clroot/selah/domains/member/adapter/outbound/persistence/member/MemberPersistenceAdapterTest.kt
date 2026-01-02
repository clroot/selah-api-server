package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberPersistenceAdapterTest : IntegrationTestBase() {
    @Autowired
    private lateinit var adapter: MemberPersistenceAdapter

    init {
        describe("MemberPersistenceAdapter") {
            describe("save") {
                context("이메일/비밀번호로 새 회원을 저장할 때") {
                    it("저장된 회원을 반환한다") {
                        // Given
                        val member = Member.createWithEmail(
                            email = Email("test@example.com"),
                            nickname = "테스트",
                            passwordHash = PasswordHash("hashed-password"),
                        )

                        // When
                        val saved = adapter.save(member)

                        // Then
                        saved.id shouldBe member.id
                        saved.email shouldBe Email("test@example.com")
                        saved.nickname shouldBe "테스트"
                        saved.hasPassword.shouldBeTrue()
                        saved.emailVerified.shouldBeFalse()
                        saved.oauthConnections.shouldBe(emptyList())
                    }
                }

                context("OAuth로 새 회원을 저장할 때") {
                    it("OAuth 연결이 함께 저장된다") {
                        // Given
                        val member = Member.createWithOAuth(
                            email = Email("oauth@example.com"),
                            nickname = "OAuth 사용자",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-123",
                        )

                        // When
                        val saved = adapter.save(member)

                        // Then
                        saved.id shouldBe member.id
                        saved.emailVerified.shouldBeTrue()
                        saved.hasPassword.shouldBeFalse()
                        saved.hasOAuthConnection.shouldBeTrue()
                        saved.oauthConnections.size shouldBe 1
                        saved.oauthConnections.first().provider shouldBe OAuthProvider.GOOGLE
                        saved.oauthConnections.first().providerId shouldBe "google-123"
                    }
                }

                context("기존 회원을 수정할 때") {
                    it("업데이트된 회원을 반환한다") {
                        // Given
                        val member = createAndSaveMember()
                        member.updateProfile(newNickname = "수정된 닉네임")

                        // When
                        val updated = adapter.save(member)

                        // Then
                        updated.id shouldBe member.id
                        updated.nickname shouldBe "수정된 닉네임"
                    }
                }

                context("OAuth 연결을 추가할 때") {
                    it("새 연결이 저장된다") {
                        // Given
                        val member = createAndSaveMember()
                        member.connectOAuth(OAuthProvider.GOOGLE, "google-456")

                        // When
                        val updated = adapter.save(member)

                        // Then
                        updated.oauthConnections.size shouldBe 1
                        updated.hasProvider(OAuthProvider.GOOGLE).shouldBeTrue()
                    }
                }

                context("OAuth 연결을 해제할 때") {
                    it("연결이 삭제된다") {
                        // Given
                        val member = createAndSaveMember()
                        member.connectOAuth(OAuthProvider.GOOGLE, "google-789")
                        adapter.save(member)

                        member.disconnectOAuth(OAuthProvider.GOOGLE)

                        // When
                        val updated = adapter.save(member)

                        // Then
                        updated.oauthConnections.shouldBe(emptyList())
                        updated.hasProvider(OAuthProvider.GOOGLE).shouldBeFalse()
                    }
                }

                context("여러 OAuth를 연결할 때") {
                    it("모든 연결이 저장된다") {
                        // Given
                        val member = Member.createWithOAuth(
                            email = Email("multi@example.com"),
                            nickname = "멀티",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-multi",
                        )
                        adapter.save(member)

                        member.connectOAuth(OAuthProvider.KAKAO, "kakao-multi")

                        // When
                        val updated = adapter.save(member)

                        // Then
                        updated.oauthConnections.size shouldBe 2
                        updated.connectedProviders shouldContainExactlyInAnyOrder setOf(
                            OAuthProvider.GOOGLE,
                            OAuthProvider.KAKAO,
                        )
                    }
                }
            }

            describe("findById") {
                context("회원이 존재할 때") {
                    it("회원을 반환한다") {
                        // Given
                        val member = createAndSaveMember()

                        // When
                        val found = adapter.findById(member.id)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe member.id
                        found.email shouldBe member.email
                    }
                }

                context("OAuth 연결이 있는 회원을") {
                    it("연결과 함께 조회한다") {
                        // Given
                        val member = Member.createWithOAuth(
                            email = Email("oauth-find@example.com"),
                            nickname = "OAuth",
                            provider = OAuthProvider.KAKAO,
                            providerId = "kakao-find",
                        )
                        adapter.save(member)

                        // When
                        val found = adapter.findById(member.id)

                        // Then
                        found.shouldNotBeNull()
                        found.oauthConnections.size shouldBe 1
                        found.oauthConnections.first().provider shouldBe OAuthProvider.KAKAO
                    }
                }

                context("회원이 존재하지 않을 때") {
                    it("null을 반환한다") {
                        // Given
                        val nonExistentId = MemberId.new()

                        // When
                        val found = adapter.findById(nonExistentId)

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findByEmail") {
                context("이메일로 회원을 찾을 때") {
                    it("회원을 반환한다") {
                        // Given
                        val email = Email("findme@example.com")
                        val member = Member.createWithEmail(
                            email = email,
                            nickname = "찾아줘",
                            passwordHash = PasswordHash("hashed"),
                        )
                        adapter.save(member)

                        // When
                        val found = adapter.findByEmail(email)

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe member.id
                        found.email shouldBe email
                    }
                }

                context("해당 이메일의 회원이 없을 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByEmail(Email("notfound@example.com"))

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("findByOAuthConnection") {
                context("OAuth 연결로 회원을 찾을 때") {
                    it("회원을 반환한다") {
                        // Given
                        val uniqueSuffix = System.currentTimeMillis()
                        val member = Member.createWithOAuth(
                            email = Email("oauth-$uniqueSuffix@example.com"),
                            nickname = "OAuth User",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-find-connection-$uniqueSuffix",
                        )
                        adapter.save(member)

                        // When
                        val found = adapter.findByOAuthConnection(
                            OAuthProvider.GOOGLE,
                            "google-find-connection-$uniqueSuffix",
                        )

                        // Then
                        found.shouldNotBeNull()
                        found.id shouldBe member.id
                        found.hasProvider(OAuthProvider.GOOGLE).shouldBeTrue()
                    }
                }

                context("여러 OAuth가 연결된 회원을") {
                    it("어느 Provider로도 찾을 수 있다") {
                        // Given
                        val uniqueSuffix = System.currentTimeMillis()
                        val member = Member.createWithOAuth(
                            email = Email("multi-oauth-$uniqueSuffix@example.com"),
                            nickname = "멀티 OAuth",
                            provider = OAuthProvider.GOOGLE,
                            providerId = "google-multi-find-$uniqueSuffix",
                        )
                        adapter.save(member)

                        member.connectOAuth(OAuthProvider.KAKAO, "kakao-multi-find-$uniqueSuffix")
                        adapter.save(member)

                        // When
                        val foundByGoogle = adapter.findByOAuthConnection(
                            OAuthProvider.GOOGLE,
                            "google-multi-find-$uniqueSuffix",
                        )
                        val foundByKakao = adapter.findByOAuthConnection(
                            OAuthProvider.KAKAO,
                            "kakao-multi-find-$uniqueSuffix",
                        )

                        // Then
                        foundByGoogle.shouldNotBeNull()
                        foundByKakao.shouldNotBeNull()
                        foundByGoogle.id shouldBe member.id
                        foundByKakao.id shouldBe member.id
                    }
                }

                context("해당 OAuth 연결이 없을 때") {
                    it("null을 반환한다") {
                        // When
                        val found = adapter.findByOAuthConnection(
                            OAuthProvider.NAVER,
                            "naver-not-exist",
                        )

                        // Then
                        found.shouldBeNull()
                    }
                }
            }

            describe("existsByEmail") {
                context("이메일이 존재할 때") {
                    it("true를 반환한다") {
                        // Given
                        val email = Email("exists@example.com")
                        val member = Member.createWithEmail(
                            email = email,
                            nickname = "존재",
                            passwordHash = PasswordHash("hashed"),
                        )
                        adapter.save(member)

                        // When
                        val exists = adapter.existsByEmail(email)

                        // Then
                        exists.shouldBeTrue()
                    }
                }

                context("이메일이 존재하지 않을 때") {
                    it("false를 반환한다") {
                        // When
                        val exists = adapter.existsByEmail(Email("notexists@example.com"))

                        // Then
                        exists.shouldBeFalse()
                    }
                }
            }
        }
    }

    private suspend fun createAndSaveMember(): Member {
        val member = Member.createWithEmail(
            email = Email("test-${System.currentTimeMillis()}@example.com"),
            nickname = "테스트 사용자",
            passwordHash = PasswordHash("hashed-password"),
        )
        return adapter.save(member)
    }
}
