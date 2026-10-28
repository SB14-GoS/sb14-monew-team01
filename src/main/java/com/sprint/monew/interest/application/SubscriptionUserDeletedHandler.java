package com.sprint.monew.interest.application;

import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionUserDeletedHandler {
    private final SubscriptionRepository subscriptionRepository;

    private final InterestRepository interestRepository;

    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        List<UUID> interestIds = subscriptionRepository.findInterestIdsByUserId(event.userId());
        subscriptionRepository.deleteAllByUserId(event.userId());
        interestIds.forEach(interestRepository::decreaseSubscriberCount);
    }
}
