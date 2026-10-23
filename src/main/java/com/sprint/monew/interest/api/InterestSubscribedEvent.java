package com.sprint.monew.interest.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterestSubscribedEvent(
        UUID subscriptionId,
        UUID userId,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant subscribedAt) {}
