package com.sprint.monew.user.api;

import java.util.UUID;

public interface UserQueryApi {

    UserSummary getSummary(UUID userId);
}
