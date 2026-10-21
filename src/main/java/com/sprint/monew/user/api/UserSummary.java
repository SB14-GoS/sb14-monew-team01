package com.sprint.monew.user.api;

import java.util.UUID;

/** 다른 모듈에 제공하는 최소 사용자 표시 정보다. */
public record UserSummary(UUID id, String nickname) {}
