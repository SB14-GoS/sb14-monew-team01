package com.sprint.monew.user.api;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email, String nickname, Instant createdAt) {}
