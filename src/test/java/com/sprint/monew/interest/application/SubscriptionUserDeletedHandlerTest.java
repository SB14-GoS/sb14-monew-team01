package com.sprint.monew.interest.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionUserDeletedHandlerTest {

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    InterestRepository interestRepository;

    @InjectMocks
    SubscriptionUserDeletedHandler handler;

    @Test
    @DisplayName("사용자가 삭제되면 구독을 지우고, 실제로 지운 관심사만 구독자 수를 줄인다")
    void onUserDeleted() {
        UUID userId = UUID.randomUUID();
        UUID deletedInterestId = UUID.randomUUID();
        UUID alreadyRemovedInterestId = UUID.randomUUID();
        given(subscriptionRepository.findInterestIdsByUserId(userId))
                .willReturn(List.of(deletedInterestId, alreadyRemovedInterestId));
        given(subscriptionRepository.deleteByUserIdAndInterestId(userId, deletedInterestId))
                .willReturn(1);
        given(subscriptionRepository.deleteByUserIdAndInterestId(userId, alreadyRemovedInterestId))
                .willReturn(0);

        handler.onUserDeleted(new UserDeletedEvent(userId));

        verify(interestRepository).decreaseSubscriberCount(deletedInterestId);
        verify(interestRepository, never()).decreaseSubscriberCount(alreadyRemovedInterestId);
    }
}
