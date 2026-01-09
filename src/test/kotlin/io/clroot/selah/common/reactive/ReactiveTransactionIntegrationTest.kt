package io.clroot.selah.common.reactive

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.application.port.outbound.LoadPrayerTopicPort
import io.clroot.selah.domains.prayer.application.port.outbound.SavePrayerTopicPort
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import io.clroot.selah.test.IntegrationTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * ReactiveSessionProvider와 ReactiveTransactionExecutor 통합 테스트
 *
 * 실제 도메인 Entity(PrayerTopic)를 사용하여 테스트합니다.
 *
 * 테스트 시나리오:
 * 1. ReactiveSessionProvider의 read/write 동작
 * 2. ReactiveTransactionExecutor의 transactional/readOnly 동작
 * 3. 트랜잭션 내 세션 재사용 확인
 * 4. 예외 발생 시 롤백 확인
 * 5. ReadOnly 트랜잭션에서 write 시도 시 예외 발생 확인
 */
@SpringBootTest
class ReactiveTransactionIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var savePrayerTopicPort: SavePrayerTopicPort

    @Autowired
    private lateinit var loadPrayerTopicPort: LoadPrayerTopicPort

    @Autowired
    private lateinit var tx: ReactiveTransactionExecutor

    private val testMemberId = MemberId.new()

    init {
        describe("ReactiveSessionProvider") {
            context("write 메서드") {
                it("새 엔티티를 저장할 수 있다") {
                    val topic = PrayerTopic.create(memberId = testMemberId, title = "테스트 기도제목")

                    val saved = savePrayerTopicPort.save(topic)

                    saved.id.shouldNotBeNull()
                    saved.title shouldBe "테스트 기도제목"
                }

                it("기존 엔티티를 업데이트할 수 있다") {
                    val topic = PrayerTopic.create(memberId = testMemberId, title = "원래 제목")
                    val saved = savePrayerTopicPort.save(topic)

                    saved.updateTitle("수정된 제목")
                    val updated = savePrayerTopicPort.save(saved)

                    updated.title shouldBe "수정된 제목"
                }
            }

            context("read 메서드") {
                it("ID로 엔티티를 조회할 수 있다") {
                    val topic = PrayerTopic.create(memberId = testMemberId, title = "조회 테스트")
                    val saved = savePrayerTopicPort.save(topic)

                    val found = loadPrayerTopicPort.findById(saved.id)

                    found.shouldNotBeNull()
                    found.title shouldBe "조회 테스트"
                }

                it("존재하지 않는 ID로 조회하면 null을 반환한다") {
                    val found =
                        loadPrayerTopicPort.findById(
                            PrayerTopicId.new(),
                        )

                    found.shouldBeNull()
                }
            }
        }

        describe("ReactiveTransactionExecutor") {
            context("transactional 블록") {
                it("여러 write 작업이 원자적으로 수행된다") {
                    val (saved1, saved2, saved3) =
                        tx.transactional {
                            val topic1 = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "첫번째"))
                            val topic2 = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "두번째"))
                            val topic3 = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "세번째"))
                            Triple(topic1, topic2, topic3)
                        }

                    loadPrayerTopicPort.findById(saved1.id).shouldNotBeNull()
                    loadPrayerTopicPort.findById(saved2.id).shouldNotBeNull()
                    loadPrayerTopicPort.findById(saved3.id).shouldNotBeNull()
                }

                it("예외 발생 시 모든 변경이 롤백된다") {
                    var savedId: PrayerTopicId? = null

                    shouldThrow<RuntimeException> {
                        tx.transactional {
                            val saved = savePrayerTopicPort.save(
                                PrayerTopic.create(testMemberId, "롤백될 기도제목"),
                            )
                            savedId = saved.id
                            throw RuntimeException("의도적 롤백 테스트")
                        }
                    }

                    // 트랜잭션 내에서 ID가 할당되었어야 함
                    savedId.shouldNotBeNull()

                    // 롤백되어 조회되지 않아야 함
                    loadPrayerTopicPort.findById(savedId).shouldBeNull()
                }

                it("중첩 transactional 블록에서 세션이 재사용된다") {
                    val result =
                        tx.transactional {
                            val outer = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "외부"))

                            // 중첩 트랜잭션 - 동일 세션 재사용
                            val inner =
                                tx.transactional {
                                    savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "내부"))
                                }

                            Pair(outer, inner)
                        }

                    loadPrayerTopicPort.findById(result.first.id).shouldNotBeNull()
                    loadPrayerTopicPort.findById(result.second.id).shouldNotBeNull()
                }

                it("값을 반환할 수 있다") {
                    val result =
                        tx.transactional {
                            val saved = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "반환 테스트"))
                            "저장됨: ${saved.title}"
                        }

                    result shouldBe "저장됨: 반환 테스트"
                }
            }

            context("readOnly 블록") {
                it("read 작업은 정상 수행된다") {
                    val saved = savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "읽기전용 테스트"))

                    val result =
                        tx.readOnly {
                            loadPrayerTopicPort.findById(saved.id)
                        }

                    result.shouldNotBeNull()
                    result.title shouldBe "읽기전용 테스트"
                }

                it("write 시도 시 ReadOnlyTransactionException이 발생한다") {
                    shouldThrow<ReadOnlyTransactionException> {
                        tx.readOnly {
                            savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "읽기전용에서 쓰기"))
                        }
                    }
                }
            }

            context("타임아웃") {
                it("기본 타임아웃으로 작업이 완료된다") {
                    val result =
                        tx.transactional {
                            savePrayerTopicPort.save(PrayerTopic.create(testMemberId, "타임아웃 테스트"))
                        }

                    result.shouldNotBeNull()
                }
            }
        }
    }
}
