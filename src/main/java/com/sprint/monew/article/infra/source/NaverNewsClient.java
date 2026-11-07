package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidNaverNewsResponseException;
import com.sprint.monew.article.infra.source.dto.NaverNewsItem;
import com.sprint.monew.article.infra.source.dto.NaverNewsResponse;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RequiredArgsConstructor
public class NaverNewsClient {

    private final int pageSize = 100;

    private final RestClient restClient;

    public List<RawArticle> fetch(Collection<String> keywords, Instant publishedSince) {
        List<RawArticle> result = new ArrayList<>();
        for (String keyword : keywords) {
            log.debug("네이버 뉴스 수집 요청 - keyword: {}", keyword);
            try {
                pages:
                for (int start = 1; start <= 901; start += 100) {
                    NaverNewsResponse response = restClient
                            .get()
                            .uri(
                                    "/search/v1/news?query={keyword}&display=100&start={start}"
                                            + "&sort=date&format=json",
                                    keyword,
                                    start)
                            .retrieve()
                            .onStatus(
                                    status -> status.isError(),
                                    (request, httpResponse) -> new DefaultResponseErrorHandler()
                                            .handleError(request.getURI(), request.getMethod(), httpResponse))
                            .body(NaverNewsResponse.class);

                    if (response == null) {
                        throw new InvalidNaverNewsResponseException(keyword);
                    }
                    List<NaverNewsItem> items = response.items();
                    if (items == null || items.isEmpty()) {
                        break;
                    }
                    for (NaverNewsItem item : items) {
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
                        /* 기사 게시일이 db에 적재된 네이버의 최신 기사보다 오래됐으면 건너뜀 */
                        Instant publishedAt = ZonedDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME)
                                .toInstant();
                        if (publishedAt.isBefore(publishedSince)) {
                            break pages;
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

                        RawArticle article =
                                new RawArticle(ArticleSource.NAVER, originallink, title, publishedAt, summary);
                        result.add(article);
                    }
                    if (items.size() < pageSize) {
                        break;
                    }
                }
            } catch (RuntimeException e) {
                String status = e instanceof RestClientResponseException httpException
                        ? httpException.getStatusCode().toString()
                        : "-";
                log.warn(
                        "네이버 뉴스 수집 실패 - source: NAVER, keyword: {}, status: {}, error: {}",
                        keyword,
                        status,
                        e.getClass().getSimpleName(),
                        e);
                throw e;
            }
        }

        return result;
    }
}
