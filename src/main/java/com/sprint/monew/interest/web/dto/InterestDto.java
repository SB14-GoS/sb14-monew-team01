package com.sprint.monew.interest.web.dto;

import com.sprint.monew.interest.domain.Interest;
import java.util.List;
import java.util.UUID;

public record InterestDto(UUID id, String name, List<String> keywords, Long subscriberCount, Boolean subscribedByMe) {
    public static InterestDto from(Interest interest, boolean subscribedByMe) {
        return new InterestDto(
                interest.getId(),
                interest.getName(),
                interest.getKeywordStrings(),
                interest.getSubscriberCount(),
                subscribedByMe);
    }
}
