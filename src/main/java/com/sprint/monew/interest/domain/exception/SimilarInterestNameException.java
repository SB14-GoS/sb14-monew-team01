package com.sprint.monew.interest.domain.exception;

import com.sprint.monew.interest.domain.InterestErrorCode;
import com.sprint.monew.interest.domain.InterestException;
import java.util.Map;

public class SimilarInterestNameException extends InterestException {
    public SimilarInterestNameException(String name, String similarName) {
        super(InterestErrorCode.SIMILAR_NAME_EXISTS, Map.of("name", name, "similarName", similarName));
    }
}
