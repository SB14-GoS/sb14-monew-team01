package com.sprint.monew.article.application;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.article.infra.source.NaverNewsClient;
import com.sprint.monew.article.infra.source.RssNewsClient;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCollectService {

    private final ArticleRepository articleRepository;
    private final NaverNewsClient naverNewsClient;
    private final RssNewsClient chosunNewsClient;
    private final RssNewsClient yeonhapNewsClient;
    private final RssNewsClient hankyungNewsClient;
    private final InterestQueryApi interestQueryApi;

    /* 배치로 돌릴거 */
    public void save() {
        /* 관심사 db 다 가져와서 키워드 기준 중복제거하고 키워드 리스트로 반환 */
        Set<String> keywords = interestQueryApi.getAllKeywords().stream()
                .flatMap(keyword -> keyword.keywords().stream())
                .collect(Collectors.toSet());

        List<RawArticle> totalResult = naverNewsClient.fetch(keywords);

        List<RawArticle> rssTotalResult = chosunNewsClient.fetch(keywords);
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

        for (RawArticle each : totalResult) {
            if (sourceUrl.add(each.sourceUrl()) /* 반환이 boolean Set에 새로 추가되면 true 추가가 안되면 false */) {
                Article article = Article.create(
                        each.title(), each.publishDate(), each.summary(), each.articleSource(), each.sourceUrl());
                result.add(article);
            }
        }
        articleRepository.saveAll(result);
    }
}
