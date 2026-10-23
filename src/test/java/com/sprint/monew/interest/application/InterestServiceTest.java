package com.sprint.monew.interest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.interest.domain.exception.InterestNotFoundException;
import com.sprint.monew.interest.domain.exception.SimilarInterestNameException;
import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import com.sprint.monew.interest.web.dto.InterestUpdateRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private InterestService interestService;

    @Test
    @DisplayName("관심사 등록 성공 - 기존 관심사와 이름이 유사하지 않으면 정상 등록된다")
    void registerInterest_Success() {
        // given: DB에 "경제"만 있고 새로 "프로그래밍"을 등록하려는 상황
        Interest existing = new Interest("경제", List.of("주식", "환율"));
        InterestRegisterRequest request = new InterestRegisterRequest("프로그래밍", List.of("자바", "스프링"));
        Interest saved = new Interest("프로그래밍", List.of("자바", "스프링"));

        given(interestRepository.findAll()).willReturn(List.of(existing));
        given(interestRepository.save(any(Interest.class))).willReturn(saved);

        // when
        InterestDto result = interestService.registerInterest(request);

        // then
        assertThat(result.name()).isEqualTo("프로그래밍");
        assertThat(result.keywords()).containsExactly("자바", "스프링");
        assertThat(result.subscribedByMe()).isFalse();
        verify(interestRepository).save(any(Interest.class));
    }

    @Test
    @DisplayName("관심사 등록 실패 - 기존 관심사 이름과 80% 이상 유사하면 SimilarInterestNameException이 발생한다")
    void registerInterest_Fail_SimilarName() {
        // given: "백엔드 개발"(6글자)과 "백엔드 개"(5글자)는 편집거리가 1이므로 유사도가 83.3% (80% 이상)
        Interest existing = new Interest("백엔드 개발", List.of("Java"));
        InterestRegisterRequest request = new InterestRegisterRequest("백엔드 개", List.of("Spring"));

        given(interestRepository.findAll()).willReturn(List.of(existing));

        // when & then
        assertThatThrownBy(() -> interestService.registerInterest(request))
                .isInstanceOf(SimilarInterestNameException.class);

        verify(interestRepository, never()).save(any(Interest.class));
    }

    @Test
    @DisplayName("관심사 키워드 수정 성공 - 이름은 유지되고 키워드 목록만 새로운 값으로 교체된다")
    void updateInterest_Success() {
        // given
        UUID interestId = UUID.randomUUID();
        Interest existing = new Interest("백엔드", List.of("자바", "스프링"));
        InterestUpdateRequest request = new InterestUpdateRequest(List.of("도커", "쿠버네티스"));

        given(interestRepository.findById(interestId)).willReturn(Optional.of(existing));

        // when
        InterestDto result = interestService.updateInterest(interestId, request);

        // then
        assertThat(result.name()).isEqualTo("백엔드");
        assertThat(result.keywords()).containsExactly("도커", "쿠버네티스");
    }

    @Test
    @DisplayName("관심사 키워드 수정 실패 - 존재하지 않는 관심사 ID면 InterestNotFoundException이 발생한다")
    void updateInterest_Fail_NotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();
        InterestUpdateRequest request = new InterestUpdateRequest(List.of("키워드"));

        given(interestRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> interestService.updateInterest(nonExistentId, request))
                .isInstanceOf(InterestNotFoundException.class);
    }
}
