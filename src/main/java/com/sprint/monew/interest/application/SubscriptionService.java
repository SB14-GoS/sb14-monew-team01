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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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

        Subscription savedSubscription;
        try {
            savedSubscription = subscriptionRepository.saveAndFlush(new Subscription(userId, interest));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateSubscriptionViolation(e)) {
                throw new AlreadySubscribedException(userId, interestId);
            }
            throw e;
        }

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

        int deleted = subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId);
        if (deleted == 0) {
            throw new SubscriptionNotFoundException(userId, interestId);
        }
        interestRepository.decreaseSubscriberCount(interestId);

        eventPublisher.publishEvent(new InterestUnsubscribedEvent(userId, interestId));
    }

    private Interest findInterestOrThrow(UUID interestId) {
        return interestRepository.findById(interestId).orElseThrow(() -> new InterestNotFoundException(interestId));
    }

    private boolean isDuplicateSubscriptionViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                if ("23505".equals(cve.getSQLState())) {
                    return true;
                }
                String constraintName = cve.getConstraintName();
                if (constraintName != null && constraintName.toLowerCase().contains("user_id")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null
                && (message.contains("user_id")
                        || message.contains("uk_subscriptions")
                        || message.contains("duplicate key"));
    }
}
