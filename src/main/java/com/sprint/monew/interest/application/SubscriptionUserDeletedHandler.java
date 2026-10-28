package com.sprint.monew.interest.application;

import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubscriptionUserDeletedHandler {
    private final SubscriptionRepository subscriptionRepository;

    private final InterestRepository interestRepository;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        UUID userId = event.userId();
        List<UUID> interestIds = subscriptionRepository.findInterestIdsByUserId(event.userId());
        for (UUID interestId : interestIds) {
            int deleted = subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId);
            if (deleted > 0) {
                interestRepository.decreaseSubscriberCount(interestId);
            }
        }
    }
}
