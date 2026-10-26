package com.sprint.monew.user.domain;

import java.util.Map;
import java.util.UUID;

public class UserNotFoundException extends UserException {
    public UserNotFoundException(UUID userId) {
        super(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
}
