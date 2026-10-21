package com.sprint.monew.interest.api;

import java.util.UUID;

public record InterestSubscribedEvent(UUID userId, UUID interestId) {}
