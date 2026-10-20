package com.sprint.monew.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** QueryDSL 조회에 쓰는 {@link JPAQueryFactory}를 등록한다. 각 모듈 infra의 {@code XxxQueryRepositoryImpl}이 주입받아 사용한다. */
@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
