package com.sprint.monew.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 PostgreSQL·MongoDB 컨테이너를 띄워 전체 애플리케이션 컨텍스트로 테스트한다.
 *
 * <pre>{@code
 * @IntegrationTest
 * class UserServiceIntegrationTest {
 *     @Autowired UserService userService;
 *     ...
 * }
 * }</pre>
 *
 * <p>같은 설정을 쓰는 테스트끼리는 컨텍스트와 컨테이너가 재사용된다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public @interface IntegrationTest {}
