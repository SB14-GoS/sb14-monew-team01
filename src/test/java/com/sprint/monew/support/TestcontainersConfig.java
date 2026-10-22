package com.sprint.monew.support;

import static org.mockito.Mockito.mock;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.user.api.UserQueryApi;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 통합 테스트용 PostgreSQL·MongoDB 컨테이너.
 * {@link ServiceConnection}이 datasource·mongodb 접속 정보를 자동으로 주입한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /**
     * Article·User 모듈의 구현체가 추가되기 전까지 CommentService가 생성될 수 있도록 한다.
     * 실제 구현체가 생기면 이 테스트 대역을 제거한다.
     */
    @Bean
    ArticleQueryApi articleQueryApi() {
        return mock(ArticleQueryApi.class);
    }

    @Bean
    ArticleCommandApi articleCommandApi() {
        return mock(ArticleCommandApi.class);
    }

    @Bean
    UserQueryApi userQueryApi() {
        return mock(UserQueryApi.class);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17");
    }

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer("mongo:8.2");
    }
}
