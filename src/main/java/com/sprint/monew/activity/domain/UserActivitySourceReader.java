package com.sprint.monew.activity.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 정합성 보정용으로 원본(PostgreSQL)에서 활동 내역을 읽는다. */
public interface UserActivitySourceReader {

    /** id 순서로 after 다음 사용자 id를 limit개 읽는다 (논리 삭제된 사용자 포함). */
    List<UUID> findUserIdsAfter(UUID after, int limit);

    /** 사용자별 구독 전체와 최근 댓글·좋아요·본 기사 10개씩. 없는 사용자는 결과에서 빠진다. */
    Map<UUID, UserActivitySource> load(List<UUID> userIds);

    Set<UUID> findExistingUserIds(Collection<UUID> userIds);
}
