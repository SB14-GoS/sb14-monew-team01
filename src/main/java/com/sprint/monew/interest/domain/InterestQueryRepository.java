package com.sprint.monew.interest.domain;

import java.util.List;

public interface InterestQueryRepository {
    List<Interest> search(InterestSearchCondition condition, InterestSearchCursor cursor);

    long countByCondition(InterestSearchCondition condition);
}
