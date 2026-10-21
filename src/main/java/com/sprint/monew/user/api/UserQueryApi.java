package com.sprint.monew.user.api;

import java.util.UUID;

/** 다른 모듈이 사용자 존재 여부와 표시 정보를 조회하는 공개 계약이다. */
public interface UserQueryApi {

    UserSummary getUser(UUID userId);
}
