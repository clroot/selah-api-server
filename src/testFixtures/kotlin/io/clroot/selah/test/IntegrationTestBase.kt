package io.clroot.selah.test

import io.clroot.selah.test.container.DatabaseTestExtension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 테스트를 위한 기본 클래스 (DescribeSpec)
 *
 * 이 클래스를 상속받으면:
 * 1. PostgreSQL TestContainer가 자동으로 시작됩니다
 * 2. Spring Boot 애플리케이션 컨텍스트가 로드됩니다
 * 3. 테스트용 프로파일(test)이 활성화됩니다
 * 4. Kotest의 DescribeSpec을 사용한 describe-context-it 스타일 테스트를 작성할 수 있습니다
 *
 * 주의사항:
 * - 통합 테스트이므로 테스트 트랜잭션을 사용하지 않습니다 (NOT_SUPPORTED)
 * - UseCase의 트랜잭션만 사용하여 실제 DB 커밋을 수행합니다
 * - 각 테스트 suite마다 DB 스키마가 재생성(create-drop)되므로 데이터 격리가 보장됩니다
 *
 * 사용 예시:
 * ```kotlin
 * @SpringBootTest
 * class MyUseCaseIntegrationTest : IntegrationTestBase() {
 *     @Autowired
 *     private lateinit var myUseCase: MyUseCase
 *
 *     init {
 *         describe("MyUseCase") {
 *             context("사용자가 존재할 때") {
 *                 it("성공 결과를 반환한다") {
 *                     val result = myUseCase.execute(command)
 *                     result shouldNotBe null
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class IntegrationTestBase : DescribeSpec() {
    init {
        extension(DatabaseTestExtension())   // TestContainer 관리 (먼저 실행되어야 함)
        extension(SpringExtension())          // Spring 컨텍스트 통합
    }
}
