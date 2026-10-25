package com.sprint.monew.interest.application;

import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.interest.api.InterestDeletedEvent;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import com.sprint.monew.interest.api.InterestQueryApi;
import com.sprint.monew.interest.api.InterestSummary;
import com.sprint.monew.interest.domain.Interest;
import com.sprint.monew.interest.domain.InterestQueryRepository;
import com.sprint.monew.interest.domain.InterestRepository;
import com.sprint.monew.interest.domain.InterestSearchCondition;
import com.sprint.monew.interest.domain.InterestSearchCursor;
import com.sprint.monew.interest.domain.SubscriptionRepository;
import com.sprint.monew.interest.domain.exception.InterestNotFoundException;
import com.sprint.monew.interest.domain.exception.InvalidInterestOrderByException;
import com.sprint.monew.interest.domain.exception.SimilarInterestNameException;
import com.sprint.monew.interest.web.dto.InterestDto;
import com.sprint.monew.interest.web.dto.InterestRegisterRequest;
import com.sprint.monew.interest.web.dto.InterestSearchRequest;
import com.sprint.monew.interest.web.dto.InterestUpdateRequest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService implements InterestQueryApi {

    private static final double SIMILARITY_THRESHOLD = 0.8;
    private static final Set<String> ALLOWED_ORDER_BY = Set.of("name", "subscriberCount");

    private final InterestRepository interestRepository;
    private final InterestQueryRepository interestQueryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InterestDto registerInterest(InterestRegisterRequest request) {
        validateNameSimilarity(request.name());
        Interest interest = new Interest(request.name(), request.keywords());
        Interest savedInterest = interestRepository.save(interest);
        return InterestDto.from(savedInterest, false);
    }

    @Transactional
    public InterestDto updateInterest(UUID interestId, InterestUpdateRequest request) {
        Interest interest = findInterestOrThrow(interestId);
        interest.updateKeywords(request.keywords());
        return InterestDto.from(interest, false);
    }

    @Transactional
    public void deleteInterest(UUID interestId) {
        Interest interest = findInterestOrThrow(interestId);
        subscriptionRepository.deleteByInterestId(interestId);
        interestRepository.delete(interest);

        eventPublisher.publishEvent(new InterestDeletedEvent(interestId));
    }

    public CursorPageResponse<InterestDto> getInterests(InterestSearchRequest request, UUID userId) {
        if (!ALLOWED_ORDER_BY.contains(request.orderBy())) {
            throw new InvalidInterestOrderByException(request.orderBy());
        }

        InterestSearchCondition condition = request.toCondition();
        InterestSearchCursor cursor = request.toCursor();

        List<Interest> interests = interestQueryRepository.search(condition, cursor);
        long totalElements = interestQueryRepository.countByCondition(condition);

        CursorPageResponse<Interest> page = CursorPageResponse.of(
                interests,
                request.limit(),
                totalElements,
                last -> cursorOf(last, request.orderBy()),
                Interest::getCreatedAt);

        List<UUID> interestIds = page.content().stream().map(Interest::getId).toList();
        Set<UUID> subscribedIds = interestIds.isEmpty()
                ? Set.of()
                : subscriptionRepository.findSubscribedInterestIds(userId, interestIds);

        List<InterestDto> dtoContent = page.content().stream()
                .map(interest -> InterestDto.from(interest, subscribedIds.contains(interest.getId())))
                .toList();

        return new CursorPageResponse<>(
                dtoContent, page.nextCursor(), page.nextAfter(), page.size(), page.totalElements(), page.hasNext());
    }

    // ========================================================================================
    // 다른 모듈과 통신하기 위한 InterestQueryApi 구현체

    @Override
    public List<InterestKeywordInfo> getAllKeywords() {
        return interestRepository.findAllWithKeywords().stream()
                .map(i -> new InterestKeywordInfo(i.getId(), i.getName(), i.getKeywordStrings()))
                .toList();
    }

    @Override
    public List<UUID> getSubscribedUserIds(UUID interestId) {
        return subscriptionRepository.findUserIdsByInterestId(interestId);
    }

    @Override
    public List<InterestSummary> getSubscribedInterests(UUID userId) {
        return subscriptionRepository.findAllByUserIdWithInterest(userId).stream()
                .map(s -> new InterestSummary(
                        s.getInterest().getId(), s.getInterest().getName()))
                .toList();
    }

    // ========================================================================================
    // 내부 헬퍼 메서드

    private void validateNameSimilarity(String newName) {
        List<Interest> existingInterests = interestRepository.findAll();
        for (Interest existing : existingInterests) {
            double similarity = calculateSimilarity(existing.getName(), newName);
            if (similarity >= SIMILARITY_THRESHOLD) {
                throw new SimilarInterestNameException(newName, existing.getName());
            }
        }
    }

    private double calculateSimilarity(String name, String newName) {
        String a = name.trim().toLowerCase();
        String b = newName.trim().toLowerCase();

        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(a, b);
        return 1.0 - ((double) distance / maxLen);
    }

    // 동적 계획법을 이용한 두 문자열 간의 최소 편입 거리를 구하는 메서드
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private Interest findInterestOrThrow(UUID interestId) {
        return interestRepository.findById(interestId).orElseThrow(() -> new InterestNotFoundException(interestId));
    }

    private String cursorOf(Interest interest, String orderBy) {
        if ("subscriberCount".equals(orderBy)) {
            return interest.getSubscriberCount() + "_" + interest.getId();
        }
        return interest.getName();
    }
}
