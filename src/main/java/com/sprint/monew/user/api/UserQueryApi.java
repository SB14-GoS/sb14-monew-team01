package com.sprint.monew.user.api;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface UserQueryApi {

    /**
     * 사용자 요약 정보를 조회한다. 논리 삭제된 사용자도 물리 삭제 전까지는 조회가 가능하다.
     *  사용자가 없으면 USER_NOT_FOUND(404 에러가 뜸)
     */
    UserSummary getSummary(UUID userId);

    /**
     * 여러 사용자의 요약 정보를 한 번에 조회한다.(목록 화면 N+1방지)
     * 존재하지 않는 ID는 MAP에서 빠진다. 빈 SET이면 빈 MAP을 반환한다.
     */
    Map<UUID, UserSummary> getSummaries(Set<UUID> userIds);

    /**
     * 활동이 가능한 사용자인지 확인을 한다.
     * 존재하고, 논리 삭제가 되지 않았으면 true, 존재하지 않거나 이미 탈퇴한 사용자면 false
     */
    boolean exists(UUID userId);
}
