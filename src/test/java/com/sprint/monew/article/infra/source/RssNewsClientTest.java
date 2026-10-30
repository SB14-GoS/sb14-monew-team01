package com.sprint.monew.article.infra.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidRssResponseException;
import com.sprint.monew.article.infra.source.dto.RssChannel;
import com.sprint.monew.article.infra.source.dto.RssItem;
import com.sprint.monew.article.infra.source.dto.RssResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RssNewsClientTest {

    private static final String FEED_URL = "https://rss.example/feed";
    private static final String ARTICLE_URL = "https://rss.example/articles/1";
    private static final String PUB_DATE = "Wed, 28 Oct 2026 15:34:44 +0900";

    private final RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
    private final RssNewsClient client = new RssNewsClient(ArticleSource.HANKYUNG, FEED_URL, restClient);

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void rejectsMissingResponseOrChannel(RssResponse response) {
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThatThrownBy(() -> client.fetch(List.of())).isInstanceOf(InvalidRssResponseException.class);
    }

    static Stream<RssResponse> invalidResponses() {
        return Stream.of(null, new RssResponse(null));
    }

    @ParameterizedTest
    @MethodSource("itemsWithoutFieldValidation")
    void leavesTitleAndLinkValidationToTheArticleDomain(RssItem item) {
        RssResponse response = new RssResponse(new RssChannel(List.of(item)));
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThat(client.fetch(List.of())).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo(item.title());
            assertThat(article.sourceUrl()).isEqualTo(item.link());
        });
    }

    static Stream<RssItem> itemsWithoutFieldValidation() {
        return Stream.of(
                new RssItem("", ARTICLE_URL, PUB_DATE, "요약"),
                new RssItem(" \t\n", ARTICLE_URL, PUB_DATE, "요약"),
                new RssItem("제목", "", PUB_DATE, "요약"),
                new RssItem("제목", " \t\n", PUB_DATE, "요약"));
    }

    @Test
    void doesNotHideDateConversionFailuresAsEmptyResults() {
        RssResponse response =
                new RssResponse(new RssChannel(List.of(new RssItem("제목", ARTICLE_URL, "invalid-date", "요약"))));
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThatThrownBy(() -> client.fetch(List.of())).isInstanceOf(DateTimeParseException.class);
    }

    @ParameterizedTest
    @MethodSource("emptyResponses")
    void returnsEmptyListWhenChannelHasNoArticles(RssResponse response) {
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThat(client.fetch(List.of())).isEmpty();
    }

    static Stream<RssResponse> emptyResponses() {
        return Stream.of(new RssResponse(new RssChannel(null)), new RssResponse(new RssChannel(List.of())));
    }

    @Test
    void restoresHtmlEntitiesInTitleAndSummary() {
        RssResponse response = new RssResponse(new RssChannel(
                List.of(new RssItem("반도체 &quot;호황&quot; &amp; 성장", ARTICLE_URL, PUB_DATE, "&#39;전망&#39; &lt;속보&gt;"))));
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThat(client.fetch(List.of())).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("반도체 \"호황\" & 성장");
            assertThat(article.summary()).isEqualTo("'전망' <속보>");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "text/xml", "application/rss+xml"})
    void readsRssXmlIntoArticles(String contentType) {
        RestClient.Builder builder = new RssNewsClientConfig().rssRestClient().mutate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String xml = """
                <rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <channel>
                    <title>뉴스 피드</title>
                    <item>
                      <title><![CDATA[경제 &quot;소식&quot;]]></title>
                      <link>https://rss.example/articles/1</link>
                      <pubDate>Wed, 28 Oct 2026 15:34:44 +0900</pubDate>
                      <dc:creator>기자</dc:creator>
                    </item>
                    <item>
                      <title>두 번째 기사</title>
                      <link>https://rss.example/articles/2</link>
                      <pubDate>Wed, 28 Oct 2026 06:34:44 +0000</pubDate>
                      <description><![CDATA[시장 &amp; 전망]]></description>
                    </item>
                  </channel>
                </rss>
                """;
        server.expect(requestTo(FEED_URL)).andRespond(withSuccess(xml, MediaType.parseMediaType(contentType)));
        RssNewsClient xmlClient = new RssNewsClient(ArticleSource.HANKYUNG, FEED_URL, builder.build());

        assertThat(xmlClient.fetch(List.of()))
                .containsExactly(
                        new RawArticle(
                                ArticleSource.HANKYUNG,
                                ARTICLE_URL,
                                "경제 \"소식\"",
                                Instant.parse("2026-10-28T06:34:44Z"),
                                ""),
                        new RawArticle(
                                ArticleSource.HANKYUNG,
                                "https://rss.example/articles/2",
                                "두 번째 기사",
                                Instant.parse("2026-10-28T06:34:44Z"),
                                "시장 & 전망"));
        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "기사 요약")
    void usesEmptySummaryOnlyWhenDescriptionIsNull(String description) {
        RssResponse response =
                new RssResponse(new RssChannel(List.of(new RssItem("기사 제목", ARTICLE_URL, PUB_DATE, description))));
        when(restClient.get().uri(FEED_URL).retrieve().onStatus(any(), any()).body(RssResponse.class))
                .thenReturn(response);

        assertThat(client.fetch(List.of()))
                .containsExactly(new RawArticle(
                        ArticleSource.HANKYUNG,
                        ARTICLE_URL,
                        "기사 제목",
                        Instant.parse("2026-10-28T06:34:44Z"),
                        description == null ? "" : description));
    }
}
