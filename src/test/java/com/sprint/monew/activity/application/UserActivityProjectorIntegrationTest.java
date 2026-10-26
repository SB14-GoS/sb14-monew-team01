package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.support.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class UserActivityProjectorIntegrationTest {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    MongoTemplate mongoTemplate;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        writer.create(userId, "user@monew.com", "닉네임", Instant.parse("2026-10-26T00:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
    }

    @Test
    @DisplayName("트랜잭션이 커밋되면 기사 조회가 활동 내역에 반영된다")
    void reflectsAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(articleViewedEvent()));

        assertThat(repository.findById(userId).orElseThrow().getArticleViews()).hasSize(1);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 활동 내역에 반영되지 않는다")
    void ignoresOnRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(articleViewedEvent());
            status.setRollbackOnly();
        });

        assertThat(repository.findById(userId).orElseThrow().getArticleViews()).isEmpty();
    }

    private ArticleViewedEvent articleViewedEvent() {
        return new ArticleViewedEvent(
                UUID.randomUUID(),
                userId,
                Instant.now(),
                UUID.randomUUID(),
                "NAVER",
                "https://news.example.com/1",
                "기사 제목",
                Instant.now(),
                "요약",
                0L,
                1L);
    }
}
