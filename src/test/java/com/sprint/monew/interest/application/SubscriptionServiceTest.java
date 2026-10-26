package com.sprint.monew.interest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("관심사 구독 성공 - 구독 저장 후 구독자 수가 증가하고 확장된 구독 이벤트가 발행된다")
    void subscribeInterest_Success() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant subscribedAt = Instant.now();

        Interest interest = new Interest("IT", List.of("AI", "클라우드"));
        ReflectionTestUtils.setField(interest, "id", interestId);
        ReflectionTestUtils.setField(interest, "subscriberCount", 1L);

        Subscription subscription = new Subscription(userId, interest);
        ReflectionTestUtils.setField(subscription, "id", subscriptionId);
        ReflectionTestUtils.setField(subscription, "createdAt", subscribedAt);

        given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
        given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
                .willReturn(false);
        given(subscriptionRepository.saveAndFlush(any(Subscription.class))).willReturn(subscription);

        // when
        SubscriptionDto result = subscriptionService.subscribeInterest(interestId, userId);

        // then
        assertThat(result.id()).isEqualTo(subscriptionId);
        assertThat(result.interestId()).isEqualTo(interestId);
        assertThat(result.interestName()).isEqualTo("IT");
        assertThat(result.interestKeywords()).containsExactly("AI", "클라우드");
        assertThat(result.interestSubscriberCount()).isEqualTo(1L);
        assertThat(result.createdAt()).isEqualTo(subscribedAt);

        verify(interestRepository).increaseSubscriberCount(interestId);
        verify(eventPublisher)
                .publishEvent(new InterestSubscribedEvent(
                        subscriptionId, userId, interestId, "IT", List.of("AI", "클라우드"), 1L, subscribedAt));
    }

    @Test
    @DisplayName("관심사 구독 실패 - 존재하지 않는 관심사 ID면 InterestNotFoundException이 발생한다")
    void subscribeInterest_Fail_InterestNotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(interestRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribeInterest(nonExistentId, userId))
                .isInstanceOf(InterestNotFoundException.class);

        verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
    }

    @Test
    @DisplayName("관심사 구독 실패 - 이미 구독 중이면 AlreadySubscribedException이 발생한다")
    void subscribeInterest_Fail_AlreadySubscribed() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Interest interest = new Interest("IT", List.of("AI"));

        given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
        given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribeInterest(interestId, userId))
                .isInstanceOf(AlreadySubscribedException.class);

        verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
        verify(interestRepository, never()).increaseSubscriberCount(any());
    }

    @Test
    @DisplayName("관심사 구독 실패 - 동시 요청으로 유니크 제약 조건 위반 발생 시 AlreadySubscribedException으로 변환된다")
    void subscribeInterest_Fail_ConcurrentUniqueConstraintViolation() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Interest interest = new Interest("IT", List.of("AI"));

        given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
        given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId))
                .willReturn(false);

        ConstraintViolationException cve = new ConstraintViolationException(
                "duplicate key value violates unique constraint",
                new SQLException("duplicate key", "23505"),
                "uk_subscriptions_user_id_interest_id");
        given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key", cve));

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribeInterest(interestId, userId))
                .isInstanceOf(AlreadySubscribedException.class);

        verify(interestRepository, never()).increaseSubscriberCount(any());
    }

    @Test
    @DisplayName("관심사 구독 취소 성공 - 구독 삭제 후 구독자 수가 감소하고 구독 취소 이벤트가 발행된다")
    void unsubscribeInterest_Success() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Interest interest = new Interest("IT", List.of("AI"));

        given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
        given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId))
                .willReturn(1);

        // when
        subscriptionService.unsubscribeInterest(interestId, userId);

        // then
        verify(subscriptionRepository).deleteByUserIdAndInterestId(userId, interestId);
        verify(interestRepository).decreaseSubscriberCount(interestId);
        verify(eventPublisher).publishEvent(new InterestUnsubscribedEvent(userId, interestId));
    }

    @Test
    @DisplayName("관심사 구독 취소 실패 - 존재하지 않는 관심사 ID면 InterestNotFoundException이 발생한다")
    void unsubscribeInterest_Fail_InterestNotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(interestRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribeInterest(nonExistentId, userId))
                .isInstanceOf(InterestNotFoundException.class);

        verify(subscriptionRepository, never()).deleteByUserIdAndInterestId(any(), any());
    }

    @Test
    @DisplayName("관심사 구독 취소 실패 - 삭제된 행이 0개(미구독 상태)면 SubscriptionNotFoundException이 발생한다")
    void unsubscribeInterest_Fail_SubscriptionNotFound() {
        // given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Interest interest = new Interest("IT", List.of("AI"));

        given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
        given(subscriptionRepository.deleteByUserIdAndInterestId(userId, interestId))
                .willReturn(0);

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribeInterest(interestId, userId))
                .isInstanceOf(SubscriptionNotFoundException.class);

        verify(interestRepository, never()).decreaseSubscriberCount(any());
    }
}
