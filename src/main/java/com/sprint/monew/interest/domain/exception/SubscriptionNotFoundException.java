package com.sprint.monew.interest.domain.exception;

import com.sprint.monew.interest.domain.InterestErrorCode;
import com.sprint.monew.interest.domain.InterestException;
import java.util.Map;
import java.util.UUID;

public class SubscriptionNotFoundException extends InterestException {

    public SubscriptionNotFoundException(UUID userId, UUID interestId) {
        super(InterestErrorCode.SUBSCRIPTION_NOT_FOUND, Map.of("userId", userId, "interestId", interestId));
    }
}
