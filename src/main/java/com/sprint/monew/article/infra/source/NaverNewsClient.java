package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidNaverNewsResponseException;
import com.sprint.monew.article.infra.source.dto.NaverNewsItem;
import com.sprint.monew.article.infra.source.dto.NaverNewsResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

@Slf4j
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
            log.info("네이버 뉴스 수집 요청 - keyword: {}", keyword);
            StringBuilder responseBody = new StringBuilder();
            try {
                NaverNewsResponse response = restClient
                        .get()
                        .uri("/search/v1/news?query={keyword}&display=100" + "&sort=date&format=json", keyword)
                        .retrieve()
                        .onStatus(status -> true, (request, httpResponse) -> {
                            responseBody.append(
                                    StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8));
                            if (httpResponse.getStatusCode().isError()) {
                                new DefaultResponseErrorHandler()
                                        .handleError(request.getURI(), request.getMethod(), httpResponse);
                            }
                        })
                        .body(NaverNewsResponse.class);

                if (response == null) {
                    throw new InvalidNaverNewsResponseException(keyword);
                }
                if (response.items() == null || response.items().isEmpty()) {
                    continue;
                }

                for (NaverNewsItem item : response.items()) {
                    /* 오리지널 링크 없으면 일반 링크로 대체 */
                    String originallink = item.originallink();
                    if (Objects.isNull(originallink) || originallink.isBlank()) {
                        originallink = item.link();
                    }
                    /* 일반링크로 대체한게 또 없으면 그냥 수집 X */
                    if (Objects.isNull(originallink) || originallink.isBlank()) {
                        log.warn("링크 누락 기사 제외 - title: {}", item.title());
                        continue;
                    }
                    /* 응답이 이런식으로 오기떄문에 걸러줘야함 html 태그 제거해줘야함
                    "title":"국감장서 '<b>북한<\/b> 지뢰' 꺼내든 박충권 &quot;장관 앞에 놔도 되냐&quot;…정동영 &quot;사...",
                    "description":"<b>북한<\/b>의 지뢰를 정동영 장관....회 외교통일위원회 종합국정감사장에 <b>북한<\/b>... ",
                     */
                    String title = item.title().replace("<b>", "").replace("</b>", "");
                    title = HtmlUtils.htmlUnescape(title);

                    String summary = item.description() == null ? "" : item.description();
                    summary = summary.replace("<b>", "").replace("</b>", "");
                    summary = HtmlUtils.htmlUnescape(summary);

                    RawArticle article = new RawArticle(
                            ArticleSource.NAVER,
                            originallink,
                            title,
                            ZonedDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME)
                                    .toInstant(),
                            summary);
                    result.add(article);
                }
            } catch (RuntimeException e) {
                Level level = e instanceof RestClientResponseException httpException
                                && httpException.getStatusCode().is4xxClientError()
                        ? Level.WARN
                        : Level.ERROR;
                log.atLevel(level)
                        .log(
                                "네이버 뉴스 수집 요청 처리 실패 - keyword: {}, response: {}, exception: {}",
                                keyword,
                                responseBody,
                                e.toString());
                throw e;
            }
        }

        return result;
    }
}
