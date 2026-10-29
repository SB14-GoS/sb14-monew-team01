package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidRssResponseException;
import com.sprint.monew.article.infra.source.dto.RssItem;
import com.sprint.monew.article.infra.source.dto.RssResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@RequiredArgsConstructor
public class RssNewsClient implements NewsSourceClient {

    private final ArticleSource source;
    private final String feedUrl;
    private final RestClient restClient;

    @Override
    public ArticleSource getSource() {
        return source;
    }

    @Override
    public List<RawArticle> fetch(Collection<String> keywords) {
        List<RawArticle> result = new ArrayList<>();

        RssResponse response = restClient.get().uri(feedUrl).retrieve().body(RssResponse.class);

        /* 응답이 비어있거나 채널이 비어있는건 비정상적 응답 */
        if (response == null || response.channel() == null) {
            throw new InvalidRssResponseException();
        }

        List<RssItem> items = response.channel().items();
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        for (RssItem item : items) {
            if (item == null
                    || item.title() == null
                    || item.title().isBlank()
                    || item.link() == null
                    || item.link().isBlank()
                    || item.pubDate() == null
                    || item.pubDate().isBlank()) {
                throw new InvalidRssResponseException();
            }

            /* html 태그 제거 */
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
    }
}
