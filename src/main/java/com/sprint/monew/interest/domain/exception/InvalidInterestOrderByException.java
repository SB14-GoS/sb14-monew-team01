package com.sprint.monew.interest.domain.exception;

import com.sprint.monew.interest.domain.InterestErrorCode;
import com.sprint.monew.interest.domain.InterestException;
import java.util.Map;

public class InvalidInterestOrderByException extends InterestException {
    public InvalidInterestOrderByException(String orderBy) {
        super(InterestErrorCode.INVALID_INTEREST_ORDER_BY, Map.of("orderBy", orderBy));
    }
}
