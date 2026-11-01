package com.sprint.monew.article.application;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        /* 관심사 db 다 가져와서 키워드 기준 중복제거하고 키워드 리스트로 반환 */
        Set<String> keywords = interestQueryApi.getAllKeywords().stream()
                .flatMap(keyword -> keyword.keywords().stream())
                .collect(Collectors.toSet());

        List<RawArticle> totalResult = naverNewsClient.fetch(keywords);

        List<RawArticle> rssTotalResult = new ArrayList<>(chosunNewsClient.fetch(keywords));
        rssTotalResult.addAll(yeonhapNewsClient.fetch(keywords));
        rssTotalResult.addAll(hankyungNewsClient.fetch(keywords));

        /* 가져온 Rss 한번에 키워드 포함되는지 검사
         * 검사 통과된 아이들만 최종 결과에 포함 */
        for (RawArticle article : rssTotalResult) {
            for (String keyword : keywords) {
                boolean titleMatched = article.title().contains(keyword);
                boolean summaryMatched = article.summary().contains(keyword);

                if (titleMatched || summaryMatched) {
                    totalResult.add(article);
                    break;
                }
            }
        }

        /* 중복되는 링크 없이 하려고 중복을 허용하지 않는 Set<> 을사용 */
        Set<String> sourceUrl = new HashSet<>();

        /* 원본링크 안겹치는 객체들 넣어놓은 리스트 */
        List<Article> result = new ArrayList<>();
        /* db에 저장된 관심사를 새로 불러온 기사랑 매핑해놓은 리스트 */
        List<ArticleInterest> mappingResult = new ArrayList<>();

        /* 받아온 기사를 기사 객체로 바꾸고, db에 저장하는 로직
         * 기사가 어떠한 관심사에 매핑되는지도 관심사-기사 매핑 테이블에 저장 */
        for (RawArticle each : totalResult) {
            /* 반환이 boolean Set에 새로 추가되면 true 추가가 안되면 false */
            if (sourceUrl.add(each.sourceUrl())) {
                /* db 조회해서 이미 원본링크 있으면 저장 안함 */
                if (articleRepository.findBySourceUrl(each.sourceUrl()).isEmpty()) {
                    Article article = Article.create(
                            each.title(), each.publishDate(), each.summary(), each.articleSource(), each.sourceUrl());
                    result.add(article);

                    /* 가져온 관심사, 키워드 매핑정보들
                     * 기사들의 제목이랑 내용에 포함되는 확인,
                     * 포함되면 관심사 매핑하여 저장 */
                    List<InterestKeywordInfo> list = interestQueryApi.getAllKeywords();
                    for (InterestKeywordInfo interest : list) {
                        for (String keyword : interest.keywords()) {
                            boolean titleMatched = each.title().contains(keyword);
                            boolean summaryMatched = each.summary().contains(keyword);

                            if (titleMatched || summaryMatched) {
                                mappingResult.add(ArticleInterest.create(article, interest.interestId()));
                                break;
                            }
                        }
                    }
                }
            }
        }

        articleRepository.saveAll(result);
        articleInterestRepository.saveAll(mappingResult);
    }
}
