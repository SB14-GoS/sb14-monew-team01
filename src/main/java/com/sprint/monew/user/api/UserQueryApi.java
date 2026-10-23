package com.sprint.monew.user.api;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public interface UserQueryApi {
    UserSummary getSummary(UUID userId);

    boolean exists(UUID userId);
}
