package com.sprint.monew.activity.web.dto;

import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionActivityDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        Instant createdAt) {

    public static SubscriptionActivityDto from(SubscriptionItem item) {
        return new SubscriptionActivityDto(
                item.subscriptionId(),
                item.interestId(),
                item.interestName(),
                item.interestKeywords(),
                item.interestSubscriberCount(),
                item.createdAt());
    }
}
