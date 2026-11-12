package com.sprint.monew.activity.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.ExplainVerbosity;
import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class UserActivityIndexInitializerTest {

    static final List<String> FIELDS = UserActivityIndexInitializer.MULTIKEY_FIELDS;

    @Autowired
    UserActivityIndexInitializer initializer;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // 다른 테스트가 컬렉션을 지우면 인덱스도 함께 사라지므로 매번 다시 만든다
        mongoTemplate.dropCollection(UserActivityDocument.class);
        initializer.ensureIndexes();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(UserActivityDocument.class);
    }

    @Test
    @DisplayName("정리 쿼리 필드마다 인덱스를 만든다")
    void createsIndexes() {
        assertThat(indexNames())
                .contains("idx_commentLikes_commentId")
                .contains("idx_commentLikes_commentUserId")
                .contains("idx_articleViews_articleId")
                .contains("idx_subscriptions_interestId");
    }

    @Test
    @DisplayName("여러 번 실행해도 인덱스가 중복되지 않는다")
    void idempotent() {
        initializer.ensureIndexes();

        assertThat(indexNames()).hasSize(FIELDS.size() + 1); // + _id_
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("FIELDS")
    @DisplayName("정리 쿼리 필터가 전체 스캔 대신 인덱스를 탄다")
    void usesIndex(String field) {
        // 배열 원소가 있는 문서를 넣어 multikey 인덱스로 실제 원소를 찾는지 본다
        String array = field.substring(0, field.indexOf('.'));
        String key = field.substring(field.indexOf('.') + 1);
        UUID target = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            UUID value = i == 7 ? target : UUID.randomUUID();
            mongoTemplate
                    .getCollection("user_activities")
                    .insertOne(new Document("_id", UUID.randomUUID())
                            .append(array, List.of(new Document(key, value), new Document(key, UUID.randomUUID()))));
        }

        Document explain = mongoTemplate
                .getCollection("user_activities")
                .find(new Document(field, target))
                .explain(ExplainVerbosity.EXECUTION_STATS);
        Document stats = explain.get("executionStats", Document.class);

        assertThat(explain.toJson()).contains("IXSCAN").doesNotContain("COLLSCAN");
        assertThat(explain.toJson()).contains("\"isMultiKey\": true");
        assertThat(stats.get("nReturned", Number.class).intValue()).isEqualTo(1);
        assertThat(stats.get("totalDocsExamined", Number.class).intValue()).isEqualTo(1);
    }

    private List<String> indexNames() {
        List<String> names = new ArrayList<>();
        mongoTemplate
                .getCollection("user_activities")
                .listIndexes()
                .forEach(index -> names.add(index.getString("name")));
        return names;
    }
}
