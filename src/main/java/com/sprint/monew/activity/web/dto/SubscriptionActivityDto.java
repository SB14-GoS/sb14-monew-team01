package com.sprint.monew.activity.web.dto;

import java.util.List;
import java.util.UUID;

public record SubscriptionActivityDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        int interestSubscriberCount,
        Integer createdAt) {}
