package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidNaverNewsResponseException;
import com.sprint.monew.article.infra.source.dto.NaverNewsItem;
import com.sprint.monew.article.infra.source.dto.NaverNewsResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@RequiredArgsConstructor
public class NaverNewsClient implements NewsSourceClient {

    private final RestClient restClient;

    @Override
    public ArticleSource getSource() {
        return ArticleSource.NAVER;
    }

    @Override
    public List<RawArticle> fetch(Collection<String> keywords) {
        List<RawArticle> result = new ArrayList<>();
        for (String keyword : keywords) {
            NaverNewsResponse response = restClient
                    .get()
                    .uri("/search/v1/news?query={keyword}&display=100" + "&sort=date&format=json", keyword)
                    .retrieve()
                    .body(NaverNewsResponse.class);

            if (response == null || response.items() == null) {
                throw new InvalidNaverNewsResponseException(keyword);
            }

            for (NaverNewsItem item : response.items()) {
                /* 응답이 이런식으로 오기떄문에 걸러줘야함 html 태그 제거해줘야함
                "title":"국감장서 '<b>북한<\/b> 지뢰' 꺼내든 박충권 &quot;장관 앞에 놔도 되냐&quot;…정동영 &quot;사...",
                "description":"<b>북한<\/b>의 지뢰를 정동영 장관....회 외교통일위원회 종합국정감사장에 <b>북한<\/b>... ",
                 */
                String title = item.title().replace("<b>", "").replace("</b>", "");
                title = HtmlUtils.htmlUnescape(title);

                String summary = item.description().replace("<b>", "").replace("</b>", "");
                summary = HtmlUtils.htmlUnescape(summary);

                RawArticle article = new RawArticle(
                        ArticleSource.NAVER,
                        item.originallink(),
                        title,
                        ZonedDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME)
                                .toInstant(),
                        summary);
                result.add(article);
            }
        }

        return result;
    }
}
