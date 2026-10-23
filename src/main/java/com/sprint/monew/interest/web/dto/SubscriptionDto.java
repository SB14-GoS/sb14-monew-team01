package com.sprint.monew.interest.web.dto;

import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.interest.domain.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        Long interestSubscriberCount,
        Instant createdAt) {
    public static SubscriptionDto from(Subscription subscription, Interest interest) {
        return new SubscriptionDto(
                subscription.getId(),
                interest.getId(),
                interest.getName(),
                interest.getKeywordStrings(),
                interest.getSubscriberCount(),
                subscription.getCreatedAt());
    }
}
