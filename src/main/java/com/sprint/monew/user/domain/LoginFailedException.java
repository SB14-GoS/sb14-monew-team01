package com.sprint.monew.user.domain;

import java.util.Map;

public class LoginFailedException extends UserException {
    public LoginFailedException() {
        super(UserErrorCode.LOGIN_FAILED, Map.of());
    }
}
