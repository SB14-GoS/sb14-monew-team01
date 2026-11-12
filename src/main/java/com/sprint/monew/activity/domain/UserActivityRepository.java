package com.sprint.monew.activity.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID> {

    /** _id 순서로 after 다음 문서의 id만 읽는다 (정합성 보정의 고아 문서 검사용). */
    @Query(value = "{ '_id': { '$gt': ?0 } }", fields = "{ '_id': 1 }", sort = "{ '_id': 1 }")
    List<UserActivityDocument> findIdsAfter(UUID after, Limit limit);
}
