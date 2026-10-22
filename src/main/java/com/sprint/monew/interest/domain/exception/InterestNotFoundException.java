package com.sprint.monew.interest.domain.exception;

import com.sprint.monew.interest.domain.InterestErrorCode;
import com.sprint.monew.interest.domain.InterestException;
import java.util.Map;
import java.util.UUID;

public class InterestNotFoundException extends InterestException {
    public InterestNotFoundException(UUID interestId) {
        super(InterestErrorCode.INTEREST_NOT_FOUND, Map.of("interestId", interestId));
    }
}
