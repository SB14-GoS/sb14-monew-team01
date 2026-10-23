package com.sprint.monew.user.domain;

import java.util.Map;

public class EmailDuplicatedException extends UserException {
    public EmailDuplicatedException(String email) {
        super(UserErrorCode.EMAIL_DUPLICATED, Map.of("email", email));
    }
}
