package com.sprint.monew.article.application;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.article.infra.source.NaverNewsClient;
import com.sprint.monew.article.infra.source.RssNewsClient;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectService {

    private final NaverNewsClient naverNewsClient;
    private final RssNewsClient chosunNewsClient;
    private final RssNewsClient yeonhapNewsClient;
    private final RssNewsClient hankyungNewsClient;
    private final InterestQueryApi interestQueryApi;
    private final ArticleMappingService articleMappingService;
    private final ArticleRepository articleRepository;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    /* 배치로 돌릴거 */
    public void save() {
        long saveStartedAt = System.nanoTime();
        log.info("기사 수집 및 저장 요청: NAVER, CHOSUN, HANKUNG, YEONHAP");
        List<InterestKeywordInfo> keywordInfos = interestQueryApi.getAllKeywords();

        /* 관심사 db 다 가져와서 키워드 기준 중복제거하고 키워드 리스트로 반환 */
        Set<String> keywords = keywordInfos.stream()
                .flatMap(keyword -> keyword.keywords().stream())
                .collect(Collectors.toSet());

        List<RawArticle> chunkedArticles = new ArrayList<>();
        int naverArticleCount = 0;
        int savedArticleCount = 0;

        long collectNaverStartedAt = System.nanoTime();
        /* db에 저장된 네이버 최신기사 하나 가져와서 게시일 파싱*/
        Instant latestNaverPublishedAt = articleRepository
                .findLatestNaverArticle()
                .map(Article::getPublishedDate)
                .orElse(Instant.MIN);
        try {
            for (String keyword : keywords) {
                List<RawArticle> naverRawArticles;
                try {
                    naverRawArticles = naverNewsClient.fetch(List.of(keyword), latestNaverPublishedAt);
                } catch (RuntimeException e) {
                    log.error("네이버 기사 수집 실패 - error: {}", e.getClass().getSimpleName());
                    break;
                }
                naverArticleCount += naverRawArticles.size();

                for (RawArticle each : naverRawArticles) {
                    chunkedArticles.add(each);

                    if (chunkedArticles.size() == 5000) {
                        savedArticleCount += articleMappingService.saveArticles(chunkedArticles, keywordInfos);
                        chunkedArticles.clear();
                    }
                }
            }
        } finally {
            log.info(
                    "네이버 기사 수집 종료 - 기사: {}건, 소요시간: {}초",
                    naverArticleCount,
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
        /* 제목이나 내용에 db에 있는 키워드와 매칭이 되는애들을 추가 */
        chunkedArticles.addAll(articleMappingService.filteringRssArticles(rssTotalResult, keywords));

        log.debug(
                "RSS 키워드 필터링 완료 - 대상: {}건, 소요시간: {}초",
                rssTotalResult.size(),
                (System.nanoTime() - stageStartedAt) / 1_000_000_000.0);

        /* 네이버 수집 후 남은 기사와 필터링한 RSS 기사를 저장 */
        savedArticleCount += articleMappingService.saveArticles(chunkedArticles, keywordInfos);

        log.debug("기사 수집 후 처리 완료 - 소요시간: {}초", (System.nanoTime() - processStartedAt) / 1_000_000_000.0);
        log.info(
                "기사 수집 및 저장 완료 - 신규 기사: {}건, 전체 소요시간: {}초",
                savedArticleCount,
                (System.nanoTime() - saveStartedAt) / 1_000_000_000.0);
    }
}
