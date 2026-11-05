package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticlesCollectedEvent;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.repository.ArticleInterestRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.article.infra.source.NaverNewsClient;
import com.sprint.monew.article.infra.source.RssNewsClient;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectService {

    private final ArticleRepository articleRepository;
    private final NaverNewsClient naverNewsClient;
    private final RssNewsClient chosunNewsClient;
    private final RssNewsClient yeonhapNewsClient;
    private final RssNewsClient hankyungNewsClient;
    private final InterestQueryApi interestQueryApi;
    private final ArticleInterestRepository articleInterestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    /* 배치로 돌릴거 */
    public void save() {
        long saveStartedAt = System.nanoTime();
        log.info("기사 수집 및 저장 요청: NAVER, CHOSUN, HANKUNG, YEONHAP");
        List<InterestKeywordInfo> keywordInfos = interestQueryApi.getAllKeywords();

        /* 관심사 db 다 가져와서 키워드 기준 중복제거하고 키워드 리스트로 반환 */
        Set<String> keywords = keywordInfos.stream()
                .flatMap(keyword -> keyword.keywords().stream())
                .collect(Collectors.toSet());

        List<RawArticle> totalResult = new ArrayList<>();
        long collectNaverStartedAt = System.nanoTime();
        try {
            totalResult.addAll(naverNewsClient.fetch(keywords));
        } catch (RuntimeException e) {
            log.error("네이버 기사 수집 실패 - error: {}", e.getClass().getSimpleName());
        } finally {
            log.info(
                    "네이버 기사 수집 종료 - 기사: {}건, 소요시간: {}초",
                    totalResult.size(),
                    (System.nanoTime() - collectNaverStartedAt) / 1_000_000_000.0);
        }

        List<RawArticle> rssTotalResult = new ArrayList<>();
        long collectChosunStartedAt = System.nanoTime();
        try {
            rssTotalResult.addAll(chosunNewsClient.fetch(keywords));
        } catch (RuntimeException e) {
            log.error("조선뉴스 전체 피드 수집 실패 - error: {}", e.getClass().getSimpleName(), e);
        } finally {
            log.debug("조선 RSS 수집 종료 - 소요시간: {}초", (System.nanoTime() - collectChosunStartedAt) / 1_000_000_000.0);
        }

        long collectYeonhapStartedAt = System.nanoTime();
        try {
            rssTotalResult.addAll(yeonhapNewsClient.fetch(keywords));
        } catch (RuntimeException e) {
            log.error("연합뉴스 전체 피드 수집 실패 - error: {}", e.getClass().getSimpleName(), e);
        } finally {
            log.debug("연합 RSS 수집 종료 - 소요시간: {}초", (System.nanoTime() - collectYeonhapStartedAt) / 1_000_000_000.0);
        }

        long collectHankyungStartedAt = System.nanoTime();
        try {
            rssTotalResult.addAll(hankyungNewsClient.fetch(keywords));
        } catch (RuntimeException e) {
            log.error("한경뉴스 전체 피드 수집 실패 - error: {}", e.getClass().getSimpleName(), e);
        } finally {
            log.debug("한경 RSS 수집 종료 - 소요시간: {}초", (System.nanoTime() - collectHankyungStartedAt) / 1_000_000_000.0);
        }

        long processStartedAt = System.nanoTime();
        long stageStartedAt = processStartedAt;
        /* 가져온 Rss 한번에 키워드 포함되는지 검사
         * 검사 통과된 아이들만 최종 결과에 포함 */
        for (RawArticle article : rssTotalResult) {
            String title = article.title().toLowerCase(Locale.ROOT);
            String summary = article.summary().toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
                boolean titleMatched = title.contains(lowerKeyword);
                boolean summaryMatched = summary.contains(lowerKeyword);

                if (titleMatched || summaryMatched) {
                    totalResult.add(article);
                    break;
                }
            }
        }

        log.debug(
                "RSS 키워드 필터링 완료 - 대상: {}건, 소요시간: {}초",
                rssTotalResult.size(),
                (System.nanoTime() - stageStartedAt) / 1_000_000_000.0);

        stageStartedAt = System.nanoTime();
        /* 중복되는 링크 없이 하려고 중복을 허용하지 않는 Set<> 을사용 */
        Set<String> sourceUrl = new HashSet<>();

        /* 원본링크 안겹치는 객체들 넣어놓은 리스트 */
        List<Article> result = new ArrayList<>();
        /* db에 저장된 관심사를 새로 불러온 기사랑 매핑해놓은 리스트 */
        List<ArticleInterest> mappingResult = new ArrayList<>();
        Map<UUID, Long> articleCounts = new HashMap<>();

        /* 1개씩 반복 조회하는거 1000개씩 반복 조회로 변경 */
        for (int i = 0; i < totalResult.size(); i += 1000) {
            int end = i + 1000;
            if (end > totalResult.size()) {
                end = totalResult.size();
            }

            List<RawArticle> chunkArticles = totalResult.subList(i, end);

            List<String> chunkUrls =
                    chunkArticles.stream().map(RawArticle::sourceUrl).distinct().toList();

            /* 이번 묶음의 URL과 일치하는 기존 기사 조회 */
            List<Article> existingArticles = articleRepository.findAllBySourceUrlIn(chunkUrls);

            for (RawArticle each : chunkArticles) {
                if (sourceUrl.add(each.sourceUrl())) {
                    /* 기존에는 여기에서 db 조회를 했음
                    개선: 미리 가져온 리스트를 조회 */
                    Optional<Article> existingArticle = existingArticles.stream()
                            .filter(article -> article.getSourceUrl().equals(each.sourceUrl()))
                            .findFirst();

                    boolean newArticle = existingArticle.isEmpty();
                    Article article;

                    /* 비어 있으면 새로운 기사객체 생성 */
                    if (newArticle) {
                        article = Article.create(
                                each.title(),
                                each.publishDate(),
                                each.summary(),
                                each.articleSource(),
                                each.sourceUrl());
                    } else {
                        /* 기사가 있으면 기사객체 꺼냄 */
                        article = existingArticle.get();
                        /* 그리고 논리삭제 됐는지 검사, 논리 삭제면 처음으로 돌아감*/
                        if (article.isDeleted()) {
                            continue;
                        }
                    }

                    /* 가져온 관심사, 키워드 매핑정보들
                     * 기사들의 제목이랑 내용에 포함되는 확인,
                     * 포함되면 관심사 매핑하여 저장 */
                    String title = each.title().toLowerCase(Locale.ROOT);
                    String summary = each.summary().toLowerCase(Locale.ROOT);
                    boolean mapped = false;
                    for (InterestKeywordInfo interest : keywordInfos) {
                        for (String keyword : interest.keywords()) {
                            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
                            boolean titleMatched = title.contains(lowerKeyword);
                            boolean summaryMatched = summary.contains(lowerKeyword);

                            if (titleMatched || summaryMatched) {
                                mapped = true;
                                /* 신규 기사이거나, 관심사 매핑이 없을때 */
                                if (newArticle
                                        || !articleInterestRepository.existsByArticle_IdAndInterestId(
                                                article.getId(), interest.interestId())) {
                                    mappingResult.add(ArticleInterest.create(article, interest.interestId()));
                                    if (newArticle) {
                                        UUID interestId = interest.interestId();
                                        long count = articleCounts.getOrDefault(interestId, 0L);
                                        articleCounts.put(interestId, count + 1);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    /* 관심사 키워드가 하나라도 매칭된 신규 기사만 저장 */
                    if (newArticle && mapped) {
                        result.add(article);
                    }
                }
            }
        }

        log.debug(
                "기사 중복 확인·객체 생성·관심사 매핑 완료 - 신규 기사: {}건, 매핑: {}건, 소요시간: {}초",
                result.size(),
                mappingResult.size(),
                (System.nanoTime() - stageStartedAt) / 1_000_000_000.0);

        stageStartedAt = System.nanoTime();
        articleRepository.saveAll(result);
        articleInterestRepository.saveAll(mappingResult);
        log.debug("기사·관심사 매핑 saveAll 호출 완료 - 소요시간: {}초", (System.nanoTime() - stageStartedAt) / 1_000_000_000.0);

        for (InterestKeywordInfo interest : keywordInfos) {
            long count = articleCounts.getOrDefault(interest.interestId(), 0L);

            if (count > 0) {
                eventPublisher.publishEvent(new ArticlesCollectedEvent(interest.interestId(), interest.name(), count));
            }
        }

        log.debug("기사 수집 후 처리 완료 - 소요시간: {}초", (System.nanoTime() - processStartedAt) / 1_000_000_000.0);
        log.info(
                "기사 수집 및 저장 완료 - 신규 기사: {}건, 전체 소요시간: {}초",
                result.size(),
                (System.nanoTime() - saveStartedAt) / 1_000_000_000.0);
    }
}
