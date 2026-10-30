package com.sprint.monew.article.infra.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleErrorCode;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.exception.InvalidNaverNewsResponseException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

class NaverNewsClientTest {

    private HttpServer server;
    private NaverNewsClient client;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body;
    private volatile String contentType = "application/json;charset=UTF-8";

    @BeforeEach
    void setUp() throws Exception {
        try (var input = getClass().getResourceAsStream("/naver/news-response.json")) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search/v1/news", exchange -> {
            requests.add(new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getRawQuery(),
                    exchange.getRequestHeaders().getFirst("X-NCP-APIGW-API-KEY-ID"),
                    exchange.getRequestHeaders().getFirst("X-NCP-APIGW-API-KEY")));

            exchange.getResponseHeaders().set("Content-Type", contentType);
            if (status == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();

        client = clientPointingTo("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private NaverNewsClient clientPointingTo(String baseUrl) {
        RestClient original = new NaverNewsClientConfig().naverRestClient("test-id", "test-secret");
        // 운영 설정의 인증 헤더·JSON 변환·타임아웃은 유지하고 요청 주소만 바꾼다.
        return new NaverNewsClient(original.mutate().baseUrl(baseUrl).build());
    }

    @Test
    @DisplayName("네이버 뉴스 클라이언트의 수집 출처는 NAVER다")
    void sourceIsNaver() {
        ArticleSource result = client.getSource();

        assertThat(result).isEqualTo(ArticleSource.NAVER);
        assertThat(requests).isEmpty();
    }

    @Test
    @DisplayName("GET 요청에 검색 조건과 API HUB 인증 헤더를 담는다")
    void sendsSearchParametersAndCredentials() {
        client.fetch(List.of("북한"));

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.method()).isEqualTo("GET");
            assertThat(URLDecoder.decode(request.rawQuery(), StandardCharsets.UTF_8))
                    .isEqualTo("query=북한&display=100&sort=date&format=json");
            assertThat(request.clientId()).isEqualTo("test-id");
            assertThat(request.clientSecret()).isEqualTo("test-secret");
        });
    }

    @Test
    @DisplayName("공백·더하기·앰퍼샌드가 있는 검색어를 하나의 query 값으로 인코딩한다")
    void encodesSpecialCharactersInKeyword() {
        client.fetch(List.of("C++ & 북한"));

        assertThat(requests.get(0).rawQuery()).startsWith("query=C%2B%2B%20%26%20%EB%B6%81%ED%95%9C&");
    }

    @Test
    @DisplayName("JSON의 모든 기사를 원문 링크·출처·UTC 발행일을 갖는 RawArticle로 변환한다")
    void convertsEveryArticleAndUsesOriginalLink() {
        List<RawArticle> result = client.fetch(List.of("북한"));

        assertThat(result)
                .containsExactly(
                        new RawArticle(
                                ArticleSource.NAVER,
                                "https://publisher.example/articles/1",
                                "북한 관련 \"뉴스\" & 소식",
                                Instant.parse("2026-10-28T01:44:00Z"),
                                "<인터뷰> 북한 소식 '오늘'"),
                        new RawArticle(
                                ArticleSource.NAVER,
                                "https://publisher.example/articles/2",
                                "두 번째 기사",
                                Instant.parse("2026-10-27T15:30:00Z"),
                                "두 번째 요약"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain;charset=UTF-8", "text/html;charset=UTF-8"})
    @DisplayName("text/plain 또는 text/html로 전달된 JSON 응답을 읽는다")
    void readsJsonWithNonJsonContentType(String responseContentType) {
        contentType = responseContentType;

        assertThat(client.fetch(List.of("북한"))).hasSize(2);
    }

    @Test
    @DisplayName("제목과 요약의 강조 태그를 제거하고 HTML 엔티티를 원래 문자로 바꾼다")
    void cleansTitleAndSummary() {
        RawArticle article = client.fetch(List.of("북한")).get(0);

        assertThat(article.title()).isEqualTo("북한 관련 \"뉴스\" & 소식");
        assertThat(article.summary()).isEqualTo("<인터뷰> 북한 소식 '오늘'");
        assertThat(article.title()).doesNotContain("<b>", "</b>", "&quot;", "&amp;");
        assertThat(article.summary()).doesNotContain("<b>", "</b>", "&lt;", "&gt;", "&#39;");
    }

    @Test
    @DisplayName("여러 키워드의 기사들을 하나의 결과 리스트에 순서대로 모은다")
    void collectsResultsFromEveryKeyword() {
        List<RawArticle> result = client.fetch(List.of("북한", "경제"));

        assertThat(requests).hasSize(2);
        assertThat(URLDecoder.decode(requests.get(0).rawQuery(), StandardCharsets.UTF_8))
                .startsWith("query=북한&");
        assertThat(URLDecoder.decode(requests.get(1).rawQuery(), StandardCharsets.UTF_8))
                .startsWith("query=경제&");
        assertThat(result)
                .extracting(RawArticle::sourceUrl)
                .containsExactly(
                        "https://publisher.example/articles/1",
                        "https://publisher.example/articles/2",
                        "https://publisher.example/articles/1",
                        "https://publisher.example/articles/2");
    }

    @Test
    @DisplayName("키워드가 없으면 네이버를 호출하지 않고 빈 리스트를 반환한다")
    void emptyKeywordsDoNotSendRequest() {
        assertThat(client.fetch(List.of())).isEmpty();
        assertThat(requests).isEmpty();
    }

    @Test
    @DisplayName("검색 결과가 0건이면 예외 없이 빈 리스트를 반환한다")
    void emptyItemsAreAValidResponse() {
        body = "{\"total\":0,\"items\":[]}";

        assertThat(client.fetch(List.of("북한"))).isEmpty();
        assertThat(requests).hasSize(1);
    }

    @Test
    @DisplayName("응답 본문이 없으면 검색 키워드를 담은 커스텀 예외를 던진다")
    void missingBodyThrowsCustomException() {
        status = 204;

        assertInvalidResponse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"items\":null}"})
    @DisplayName("items가 누락되거나 null이면 빈 결과로 처리하고 다음 키워드도 요청한다")
    void missingItemsAreEmptyResults(String responseBody) {
        body = responseBody;

        assertThat(client.fetch(List.of("북한", "경제"))).isEmpty();
        assertThat(requests).hasSize(2);
    }

    @Test
    void nullJsonBodyIsAnInvalidResponse() {
        body = "null";

        assertInvalidResponse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\"description\":null,", "\"description\":\"\","})
    @DisplayName("선택값인 기사 요약이 없거나 null이어도 빈 문자열로 수집한다")
    void missingDescriptionIsAnEmptySummary(String descriptionField) {
        body = """
                {"items":[{
                  "title":"기사 제목",
                  "originallink":"https://publisher.example/articles/1",
                  %s
                  "pubDate":"Wed, 28 Oct 2026 10:44:00 +0900"
                }]}
                """.formatted(descriptionField);

        assertThat(client.fetch(List.of("경제"))).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("기사 제목");
            assertThat(article.summary()).isEmpty();
        });
    }

    private void assertInvalidResponse() {
        assertThatThrownBy(() -> client.fetch(List.of("북한")))
                .isInstanceOfSatisfying(InvalidNaverNewsResponseException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ArticleErrorCode.INVALID_NAVER_NEWS_RESPONSE);
                    assertThat(exception.getErrorCode().getStatus().value()).isEqualTo(502);
                    assertThat(exception.getDetails()).containsEntry("keyword", "북한");
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 500})
    @DisplayName("인증 실패·호출 한도 초과·서버 오류를 빈 검색 결과로 숨기지 않는다")
    void propagatesHttpErrors(int responseStatus) {
        status = responseStatus;
        body = "{\"error\":{\"message\":\"upstream failure\"}}";

        assertThatThrownBy(() -> client.fetch(List.of("북한")))
                .isInstanceOfSatisfying(
                        RestClientResponseException.class,
                        exception ->
                                assertThat(exception.getStatusCode().value()).isEqualTo(responseStatus));
        assertThat(requests).isNotEmpty();
    }

    @Test
    @DisplayName("해석할 수 없는 JSON이면 오류를 그대로 전달한다")
    void malformedJsonIsNotAnEmptyResult() {
        body = "{broken-json";

        assertThatThrownBy(() -> client.fetch(List.of("북한"))).isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("발행일 형식이 잘못되면 오류를 그대로 전달한다")
    void malformedDateIsNotAnEmptyResult() {
        body = body.replace("Wed, 28 Oct 2026 10:44:00 +0900", "invalid-date");

        assertThatThrownBy(() -> client.fetch(List.of("북한"))).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "NAVER_SIMULATION_URL", matches = ".+")
    @DisplayName("Postman 로컬 시뮬레이션과 실제 HTTP 통신으로 기사를 수집한다")
    void fetchesFromPostmanSimulation() {
        NaverNewsClient simulated = clientPointingTo(System.getenv("NAVER_SIMULATION_URL"));

        assertThat(simulated.fetch(List.of("북한"))).hasSize(2).first().satisfies(article -> {
            assertThat(article.articleSource()).isEqualTo(ArticleSource.NAVER);
            assertThat(article.sourceUrl()).isEqualTo("https://publisher.example/articles/1");
            assertThat(article.title()).isEqualTo("북한 관련 \"뉴스\" & 소식");
        });
    }

    private record Request(String method, String rawQuery, String clientId, String clientSecret) {}
}
