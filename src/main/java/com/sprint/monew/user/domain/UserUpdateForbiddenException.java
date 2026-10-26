package com.sprint.monew.user.domain;

import java.util.Map;
import java.util.UUID;

public class UserUpdateForbiddenException extends UserException {
    public UserUpdateForbiddenException(UUID userId) {
        super(UserErrorCode.USER_UPDATE_FORBIDDEN, Map.of("userId", userId));
    }
}
