package io.clroot.selah.common.application

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 트랜잭션 커밋 후 실행할 작업을 등록하는 헬퍼 함수
 *
 * 트랜잭션이 활성화된 경우 커밋 후에 블록을 실행하고,
 * 트랜잭션이 없는 경우 즉시 실행합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @Transactional
 * suspend fun createUser(command: CreateUserCommand) {
 *     val user = userRepository.save(User.create(command))
 *
 *     // 트랜잭션 커밋 후 이메일 발송
 *     afterCommit {
 *         applicationScope.launch {
 *             emailService.sendWelcomeEmail(user.email)
 *         }
 *     }
 * }
 * ```
 */
inline fun afterCommit(crossinline block: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    block()
                }
            },
        )
    } else {
        block()
    }
}
