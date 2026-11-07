package com.sprint.monew.article.application;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.infra.source.NaverNewsClient;
import com.sprint.monew.article.infra.source.RssNewsClient;
import com.sprint.monew.interest.api.InterestKeywordInfo;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        /* 제목이나 내용에 db에 있는 키워드와 매칭이 되는애들을 추가 */
        totalResult.addAll(articleMappingService.filteringRssArticles(rssTotalResult, keywords));

        log.debug(
                "RSS 키워드 필터링 완료 - 대상: {}건, 소요시간: {}초",
                rssTotalResult.size(),
                (System.nanoTime() - stageStartedAt) / 1_000_000_000.0);

        int savedArticleCount = articleMappingService.saveChunk(totalResult, keywordInfos);

        log.debug("기사 수집 후 처리 완료 - 소요시간: {}초", (System.nanoTime() - processStartedAt) / 1_000_000_000.0);
        log.info(
                "기사 수집 및 저장 완료 - 신규 기사: {}건, 전체 소요시간: {}초",
                savedArticleCount,
                (System.nanoTime() - saveStartedAt) / 1_000_000_000.0);
    }
}
