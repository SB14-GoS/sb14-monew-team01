package com.sprint.monew.interest.api;

import java.util.List;
import java.util.UUID;

public interface InterestQueryApi {

    // 모든 관심사의 키워드 정보를 조회하는 메서드
    List<InterestKeywordInfo> getAllKeywords();

    // 특정 관심사를 구독 중인 모든 사용자의 ID 목록을 조회하는 메서드
    List<UUID> getSubscribedUserIds(UUID interestId);

    // 특정 사용자가 구독 중인 관심사를 조회하는 메서드
    List<InterestSummary> getSubscribedInterests(UUID userId);
}
