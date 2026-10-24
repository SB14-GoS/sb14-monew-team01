package com.sprint.monew.interest.application;

import com.sprint.monew.interest.api.InterestSubscribedEvent;
import com.sprint.monew.interest.api.InterestUnsubscribedEvent;
import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.Subscription;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.interest.domain.exception.AlreadySubscribedException;
import com.sprint.monew.interest.domain.exception.InterestNotFoundException;
import com.sprint.monew.interest.domain.exception.SubscriptionNotFoundException;
import com.sprint.monew.interest.web.dto.SubscriptionDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {
    private final InterestRepository interestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SubscriptionDto subscribeInterest(UUID interestId, UUID userId) {
        Interest interest = findInterestOrThrow(interestId);

        if (subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)) {
            throw new AlreadySubscribedException(userId, interestId);
        }

        Subscription savedSubscription = subscriptionRepository.save(new Subscription(userId, interest));
        interestRepository.increaseSubscriberCount(interestId);

        Interest updatedInterest = findInterestOrThrow(interestId);

        eventPublisher.publishEvent(new InterestSubscribedEvent(
                savedSubscription.getId(),
                userId,
                updatedInterest.getId(),
                updatedInterest.getName(),
                updatedInterest.getKeywordStrings(),
                updatedInterest.getSubscriberCount(),
                savedSubscription.getCreatedAt()));
        return SubscriptionDto.from(savedSubscription, updatedInterest);
    }

    @Transactional
    public void unsubscribeInterest(UUID interestId, UUID userId) {
        findInterestOrThrow(interestId);

        Subscription subscription = subscriptionRepository
                .findByUserIdAndInterestId(userId, interestId)
                .orElseThrow(() -> new SubscriptionNotFoundException(userId, interestId));

        subscriptionRepository.delete(subscription);
        interestRepository.decreaseSubscriberCount(interestId);

        eventPublisher.publishEvent(new InterestUnsubscribedEvent(userId, interestId));
    }

    private Interest findInterestOrThrow(UUID interestId) {
        return interestRepository.findById(interestId).orElseThrow(() -> new InterestNotFoundException(interestId));
    }
}
