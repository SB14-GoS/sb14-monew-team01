package com.sprint.monew.activity.infra;

import com.sprint.monew.activity.domain.UserActivityDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * 여러 사용자 문서를 한 번에 정리하는 updateMulti 쿼리용 multikey 인덱스를 만든다.
 *
 * <p>인덱스가 없으면 정리 쿼리마다 컬렉션 전체를 훑는다(COLLSCAN). 이미 있는 인덱스는 그대로 두므로 기동할 때마다 실행해도 된다.
 * auto-index-creation 대신 직접 만드는 이유는 어떤 인덱스가 언제 생기는지 코드에서 바로 보이게 하기 위해서다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityIndexInitializer {

    /** 필드 경로 = 정리 쿼리의 필터 (UserActivityMongoWriter.remove*FromAll) */
    static final List<String> MULTIKEY_FIELDS = List.of(
            "commentLikes.commentId",
            "commentLikes.commentUserId",
            "articleViews.articleId",
            "subscriptions.interestId");

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(UserActivityDocument.class);
        for (String field : MULTIKEY_FIELDS) {
            String name = indexName(field);
            indexOps.createIndex(new Index().on(field, Sort.Direction.ASC).named(name));
            log.info("활동 내역 인덱스 확인: {}", name);
        }
    }

    static String indexName(String field) {
        return "idx_" + field.replace('.', '_');
    }
}
