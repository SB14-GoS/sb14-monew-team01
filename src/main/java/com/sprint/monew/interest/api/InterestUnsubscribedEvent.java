package com.sprint.monew.interest.api;

import java.util.UUID;

public record InterestUnsubscribedEvent(UUID userId, UUID interestId) {}
