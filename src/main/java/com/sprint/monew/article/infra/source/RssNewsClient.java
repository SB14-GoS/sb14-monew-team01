package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidRssResponseException;
import com.sprint.monew.article.infra.source.dto.RssItem;
import com.sprint.monew.article.infra.source.dto.RssResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
public class RssNewsClient implements NewsSourceClient {

    private final ArticleSource source;
    private final List<String> feedUrls;
    private final RestClient restClient;

    @Override
    public ArticleSource getSource() {
        return source;
    }

    @Override
    public List<RawArticle> fetch(Collection<String> keywords) {
        List<RawArticle> result = new ArrayList<>();
        RuntimeException lastFailure = null;
        boolean fetched = false;
        for (String feedUrl : feedUrls) {
            try {
                result.addAll(fetchFeed(feedUrl));
                fetched = true;
            } catch (RuntimeException e) {
                // 한 피드가 실패해도 다른 분야의 기사는 계속 수집한다.
                lastFailure = e;
            }
        }
        if (!fetched && lastFailure != null) {
            throw lastFailure;
        }
        return result;
    }

    private List<RawArticle> fetchFeed(String feedUrl) {
        log.info("RSS 뉴스 수집 요청 - source: {}, url: {}", source, feedUrl);
        List<RawArticle> result = new ArrayList<>();

        /* 로그로 찍힐 외부 응답 원문 */
        StringBuilder responseBody = new StringBuilder();

        try {
            RssResponse response = restClient
                    .get()
                    .uri(feedUrl)
                    .retrieve()
                    .onStatus(status -> true, (request, httpResponse) -> {
                        responseBody.append(StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8));
                        if (httpResponse.getStatusCode().isError()) {
                            new DefaultResponseErrorHandler()
                                    .handleError(request.getURI(), request.getMethod(), httpResponse);
                        }
                    })
                    .body(RssResponse.class);

            /* 응답이 비어있거나 채널이 비어있는건 비정상적 응답 */
            if (response == null || response.channel() == null) {
                throw new InvalidRssResponseException();
            }

            List<RssItem> items = response.channel().items();
            if (items == null || items.isEmpty()) {
                return List.of();
            }
            for (RssItem item : items) {
                /* html 복구 */
                String title = HtmlUtils.htmlUnescape(item.title());
                String summary = HtmlUtils.htmlUnescape(item.description() == null ? "" : item.description());

                RawArticle article = new RawArticle(
                        source,
                        item.link(),
                        title,
                        ZonedDateTime.parse(item.pubDate(), DateTimeFormatter.RFC_1123_DATE_TIME)
                                .toInstant(),
                        summary);
                result.add(article);
            }

            return result;
        } catch (RuntimeException e) {
            Level level = e instanceof RestClientResponseException httpException
                            && httpException.getStatusCode().is4xxClientError()
                    ? Level.WARN
                    : Level.ERROR;
            log.atLevel(level)
                    .log(
                            "RSS 뉴스 수집 요청 처리 실패 - source: {}, url: {}, response: {}, exception: {}",
                            source,
                            feedUrl,
                            responseBody,
                            e.toString());
            throw e;
        }
    }
}
