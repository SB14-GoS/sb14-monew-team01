package com.sprint.monew.activity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.activity.domain.ActivityOutbox;
import com.sprint.monew.activity.domain.ActivityOutboxRepository;
import com.sprint.monew.activity.domain.ActivityOutboxStatus;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivityWriter;
import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.comment.api.CommentCreatedEvent;
import com.sprint.monew.comment.api.CommentUpdatedEvent;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ActivityOutboxIntegrationTest {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    UserActivityWriter writer;

    @Autowired
    UserActivityRepository repository;

    @Autowired
    ActivityOutboxRepository outboxRepository;

    @Autowired
    ActivityOutboxRelay relay;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 다른 테스트가 발행한 이벤트가 섞이지 않게 비운다
        outboxRepository.deleteAll();
        writer.create(userId, "user@monew.com", "닉네임", Instant.parse("2026-10-26T00:00:00Z"));
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        mongoTemplate.dropCollection(UserActivityDocument.class);
    }

    @Test
    @DisplayName("커밋되면 outbox에 남고, relay가 돌아야 활동 내역에 반영된다")
    void recordsOnCommitAndRelays() {
        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(articleViewedEvent()));

        assertThat(outboxRepository.countByStatus(ActivityOutboxStatus.PENDING)).isEqualTo(1);
        assertThat(repository.findById(userId).orElseThrow().getArticleViews()).isEmpty();

        ActivityOutboxRelay.RelayResult result = relay.relayBatch();

        assertThat(result.done()).isEqualTo(1);
        assertThat(repository.findById(userId).orElseThrow().getArticleViews()).hasSize(1);
        assertThat(outboxRepository.findAll()).singleElement().satisfies(outbox -> {
            assertThat(outbox.getStatus()).isEqualTo(ActivityOutboxStatus.DONE);
            assertThat(outbox.getProcessedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("롤백되면 outbox에도 남지 않는다")
    void discardsOnRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(articleViewedEvent());
            status.setRollbackOnly();
        });

        relay.relayBatch();

        assertThat(outboxRepository.count()).isZero();
        assertThat(repository.findById(userId).orElseThrow().getArticleViews()).isEmpty();
    }

    @Test
    @DisplayName("트랜잭션 밖에서 발행해도 outbox에 저장된다")
    void recordsWithoutTransaction() {
        publisher.publishEvent(articleViewedEvent());

        assertThat(outboxRepository.findAll())
                .singleElement()
                .extracting(ActivityOutbox::getEventType)
                .isEqualTo("ArticleViewedEvent");
    }

    @Test
    @DisplayName("저장 순서대로 반영한다")
    void relaysInOrder() {
        UUID commentId = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(new CommentCreatedEvent(
                    commentId, UUID.randomUUID(), "기사", userId, "닉네임", "처음 내용", 0L, Instant.now()));
            publisher.publishEvent(new CommentUpdatedEvent(commentId, userId, "고친 내용"));
        });

        relay.relayBatch();

        assertThat(repository.findById(userId).orElseThrow().getComments())
                .singleElement()
                .extracting(UserActivityDocument.CommentItem::content)
                .isEqualTo("고친 내용");
    }

    @Test
    @DisplayName("DONE 저장 전에 멈춰 같은 이벤트가 다시 반영돼도 결과가 같다")
    void replayIsIdempotent() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(articleViewedEvent());
            publisher.publishEvent(new CommentCreatedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()));
        });
        relay.relayBatch();

        // relay가 MongoDB에 반영한 뒤 DONE을 커밋하지 못하고 죽은 상황
        jdbcTemplate.update("update activity_outbox set status = 'PENDING', processed_at = null");
        ActivityOutboxRelay.RelayResult replay = relay.relayBatch();

        assertThat(replay.done()).isEqualTo(2);
        UserActivityDocument document = repository.findById(userId).orElseThrow();
        assertThat(document.getArticleViews()).hasSize(1);
        assertThat(document.getComments()).hasSize(1);
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
