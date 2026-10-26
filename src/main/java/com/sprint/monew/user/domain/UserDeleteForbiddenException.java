package com.sprint.monew.user.domain;

import java.util.Map;
import java.util.UUID;

public class UserDeleteForbiddenException extends UserException {
    public UserDeleteForbiddenException(UUID userId) {
        super(UserErrorCode.USER_DELETE_FORBIDDEN, Map.of("userId", userId));
    }
}
