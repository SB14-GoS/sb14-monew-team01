package com.sprint.monew.activity.domain.exception;

import com.sprint.monew.activity.domain.ActivityErrorCode;
import java.util.Map;
import java.util.UUID;

public class UserActivityNotFoundException extends ActivityException {

    public UserActivityNotFoundException(UUID userId) {
        super(ActivityErrorCode.USER_ACTIVITY_NOT_FOUND, Map.of("userId", userId));
    }
}
