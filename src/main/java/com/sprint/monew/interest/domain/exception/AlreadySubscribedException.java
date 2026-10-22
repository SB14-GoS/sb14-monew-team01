package com.sprint.monew.interest.domain.exception;

import com.sprint.monew.interest.domain.InterestErrorCode;
import com.sprint.monew.interest.domain.InterestException;
import java.util.Map;
import java.util.UUID;

public class AlreadySubscribedException extends InterestException {
    public AlreadySubscribedException(UUID userId, UUID interestId) {
        super(InterestErrorCode.ALREADY_SUBSCRIBED, Map.of("userId", userId, "interestId", interestId));
    }
}
