package io.clroot.selah.common.application

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.transaction.support.TransactionSynchronizationManager

class TransactionHooksTest : DescribeSpec({

    afterEach {
        // 테스트 후 정리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    describe("afterCommit") {
        context("트랜잭션이 활성화되지 않은 경우") {
            it("블록이 즉시 실행된다") {
                var executed = false

                afterCommit {
                    executed = true
                }

                executed shouldBe true
            }

            it("블록 내 값이 정상적으로 캡처된다") {
                val results = mutableListOf<String>()

                afterCommit {
                    results.add("first")
                }
                afterCommit {
                    results.add("second")
                }

                results shouldBe listOf("first", "second")
            }
        }

        context("트랜잭션이 활성화된 경우") {
            it("블록이 즉시 실행되지 않고 등록된다") {
                TransactionSynchronizationManager.initSynchronization()

                var executed = false

                afterCommit {
                    executed = true
                }

                // 아직 실행되지 않음
                executed shouldBe false

                // 등록된 synchronization이 있어야 함
                TransactionSynchronizationManager.getSynchronizations().size shouldBe 1
            }

            it("여러 afterCommit 호출 시 모두 등록된다") {
                TransactionSynchronizationManager.initSynchronization()

                val results = mutableListOf<Int>()

                afterCommit { results.add(1) }
                afterCommit { results.add(2) }
                afterCommit { results.add(3) }

                // 아직 실행되지 않음
                results shouldBe emptyList()

                // 3개의 synchronization이 등록됨
                TransactionSynchronizationManager.getSynchronizations().size shouldBe 3
            }

            it("afterCommit 콜백이 수동으로 호출되면 블록이 실행된다") {
                TransactionSynchronizationManager.initSynchronization()

                var executed = false
                var capturedValue = ""

                afterCommit {
                    executed = true
                    capturedValue = "hello"
                }

                // 아직 실행되지 않음
                executed shouldBe false

                // 수동으로 afterCommit 호출 (트랜잭션 커밋 시뮬레이션)
                TransactionSynchronizationManager.getSynchronizations().forEach { sync ->
                    sync.afterCommit()
                }

                // 이제 실행됨
                executed shouldBe true
                capturedValue shouldBe "hello"
            }

            it("등록 순서대로 실행된다") {
                TransactionSynchronizationManager.initSynchronization()

                val executionOrder = mutableListOf<String>()

                afterCommit { executionOrder.add("first") }
                afterCommit { executionOrder.add("second") }
                afterCommit { executionOrder.add("third") }

                // 수동으로 afterCommit 호출
                TransactionSynchronizationManager.getSynchronizations().forEach { sync ->
                    sync.afterCommit()
                }

                executionOrder shouldBe listOf("first", "second", "third")
            }

            it("외부 변수를 올바르게 캡처한다") {
                TransactionSynchronizationManager.initSynchronization()

                val email = "test@example.com"
                val nickname = "TestUser"
                var capturedEmail = ""
                var capturedNickname = ""

                afterCommit {
                    capturedEmail = email
                    capturedNickname = nickname
                }

                // 수동으로 afterCommit 호출
                TransactionSynchronizationManager.getSynchronizations().forEach { sync ->
                    sync.afterCommit()
                }

                capturedEmail shouldBe "test@example.com"
                capturedNickname shouldBe "TestUser"
            }
        }
    }
})
