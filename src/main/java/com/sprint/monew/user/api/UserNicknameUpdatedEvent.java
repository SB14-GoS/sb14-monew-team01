package com.sprint.monew.user.api;

import java.util.UUID;

public record UserNicknameUpdatedEvent(UUID userId, String nickname) {}
