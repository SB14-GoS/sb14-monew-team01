package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticlesCollectedEvent;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.repository.ArticleInterestIdPair;
import com.sprint.monew.article.domain.repository.ArticleInterestRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleMappingService {

    private final ArticleRepository articleRepository;
    private final ArticleInterestRepository articleInterestRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final int chunkSize = 1000;

    @Transactional
    public int saveChunk(List<RawArticle> totalResult, List<InterestKeywordInfo> keywordInfos) {
        long stageStartedAt = System.nanoTime();
        /* 중복되는 링크 없이 하려고 중복을 허용하지 않는 Set<> 을사용 */
        Set<String> sourceUrl = new HashSet<>();

        /* 원본링크 안겹치는 객체들 넣어놓은 리스트 */
        List<Article> result = new ArrayList<>();
        /* db에 저장된 관심사를 새로 불러온 기사랑 매핑해놓은 리스트 */
        List<ArticleInterest> mappingResult = new ArrayList<>();
        Map<UUID, Long> articleCounts = new HashMap<>();

        /* 기사 새로 저장, 관심사 연결 추가*/
        /* 1개씩 반복 조회하는거 1000개씩 반복 조회로 변경 */
        for (int i = 0; i < totalResult.size(); i += chunkSize) {
            int end = i + chunkSize;
            if (end > totalResult.size()) {
                end = totalResult.size();
            }

            List<RawArticle> chunkArticles = totalResult.subList(i, end);

            List<String> chunkUrls =
                    chunkArticles.stream().map(RawArticle::sourceUrl).distinct().toList();

            /* 이번 묶음의 URL과 일치하는 기존 기사 조회 */
            List<Article> existingArticles = articleRepository.findAllBySourceUrlIn(chunkUrls);
            /* 1000개 단위로  논리삭제 된 애들은 거르기*/
            List<UUID> existingArticleIds = existingArticles.stream()
                    .filter(article1 -> !article1.isDeleted())
                    .map(article1 -> article1.getId())
                    .toList();
            /* (기사id, 관심사id)들을 리스트로 가져와서 set에 저장 */
            Set<ArticleInterestIdPair> existingMappings = new HashSet<>();
            if (!existingArticleIds.isEmpty()) {
                existingMappings.addAll(articleInterestRepository.findInterestIdPairsByArticleIds(existingArticleIds));
            }
            /* 가져온 기사로 db에 있는 url 중복 거름 */
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

                    List<UUID> matchedInterestIds = findInterestIdsByKeywordMatch(each, keywordInfos);
                    for (UUID interestId : matchedInterestIds) {
                        /* 신규 기사이거나, 관심사 매핑이 없을때 */
                        if (newArticle
                                || !existingMappings.contains(new ArticleInterestIdPair(article.getId(), interestId))) {
                            mappingResult.add(ArticleInterest.create(article, interestId));
                            if (newArticle) {
                                long count = articleCounts.getOrDefault(interestId, 0L);
                                articleCounts.put(interestId, count + 1);
                            }
                        }
                    }
                    /* 관심사 키워드가 하나라도 매칭된 신규 기사만 저장 */
                    if (newArticle && !matchedInterestIds.isEmpty()) {
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

        return result.size();
    }

    /* RSS 기사 중 제목이나 요약에 키워드가 있는 기사만 반환한다. */
    public List<RawArticle> filteringRssArticles(List<RawArticle> rssTotalResult, Set<String> keywords) {
        List<RawArticle> matchedArticles = new ArrayList<>();

        for (RawArticle article : rssTotalResult) {
            String title = article.title().toLowerCase(Locale.ROOT);
            String summary = article.summary().toLowerCase(Locale.ROOT);

            for (String keyword : keywords) {
                String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
                boolean titleMatched = title.contains(lowerKeyword);
                boolean summaryMatched = summary.contains(lowerKeyword);

                if (titleMatched || summaryMatched) {
                    matchedArticles.add(article);
                    break;
                }
            }
        }
        return matchedArticles;
    }

    /* 가져온 관심사, 키워드 매핑정보들
     * 기사들의 제목이랑 내용에 포함되는 확인,
     * 포함되면 관심사 매핑하여 저장 */
    private List<UUID> findInterestIdsByKeywordMatch(RawArticle article, List<InterestKeywordInfo> interests) {
        String title = article.title().toLowerCase(Locale.ROOT);
        String summary = article.summary().toLowerCase(Locale.ROOT);
        List<UUID> matchedInterestIds = new ArrayList<>();

        for (InterestKeywordInfo interest : interests) {
            for (String keyword : interest.keywords()) {
                String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
                if (title.contains(lowerKeyword) || summary.contains(lowerKeyword)) {
                    matchedInterestIds.add(interest.interestId());
                    break;
                }
            }
        }
        return matchedInterestIds;
    }
}
