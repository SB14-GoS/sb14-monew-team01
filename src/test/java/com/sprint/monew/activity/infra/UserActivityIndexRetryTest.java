package com.sprint.monew.activity.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.sprint.monew.activity.domain.UserActivityDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@ExtendWith(MockitoExtension.class)
class UserActivityIndexRetryTest {

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    IndexOperations indexOps;

    private UserActivityIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        given(mongoTemplate.indexOps(UserActivityDocument.class)).willReturn(indexOps);
        initializer = new UserActivityIndexInitializer(mongoTemplate);
    }

    @Test
    @DisplayName("기동 시 MongoDB가 실패해도 예외를 던지지 않고, 다음 재시도에서 성공하면 더 시도하지 않는다")
    void retriesUntilReady() {
        willThrow(new DataAccessResourceFailureException("mongo down"))
                .given(indexOps)
                .createIndex(any(Index.class));

        assertThatCode(initializer::ensureIndexesOnStartup).doesNotThrowAnyException();
        assertThat(initializer.isReady()).isFalse();

        reset(indexOps);
        initializer.retryUntilReady();
        assertThat(initializer.isReady()).isTrue();

        reset(indexOps);
        initializer.retryUntilReady();
        verify(indexOps, never()).createIndex(any(Index.class));
    }
}
