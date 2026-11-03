package com.sprint.monew.article.infra.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.domain.ArticleSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class RssNewsClientConfigTest {

    @Test
    void registersAllKoreanTextFeedsWithTheSharedRestClient() {
        Map<ArticleSource, Integer> expectedFeedCounts = Map.of(
                ArticleSource.HANKYUNG, 12,
                ArticleSource.CHOSUN, 9,
                ArticleSource.YEONHAP, 15);

        new ApplicationContextRunner()
                .withUserConfiguration(RssNewsClientConfig.class)
                .withInitializer(context -> {
                    try {
                        new YamlPropertySourceLoader()
                                .load("application", new ClassPathResource("application.yml"))
                                .forEach(source -> context.getEnvironment()
                                        .getPropertySources()
                                        .addLast(source));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var clients = context.getBeansOfType(NewsSourceClient.class);
                    assertThat(clients).containsOnlyKeys("hankyungNewsClient", "chosunNewsClient", "yeonhapNewsClient");
                    assertThat(clients.values())
                            .extracting(NewsSourceClient::getSource)
                            .containsExactlyInAnyOrderElementsOf(expectedFeedCounts.keySet());
                    RestClient restClient = context.getBean("rssRestClient", RestClient.class);
                    for (NewsSourceClient client : clients.values()) {
                        @SuppressWarnings("unchecked")
                        List<String> feedUrls = (List<String>) ReflectionTestUtils.getField(client, "feedUrls");
                        assertThat(feedUrls)
                                .hasSize(expectedFeedCounts.get(client.getSource()))
                                .doesNotHaveDuplicates()
                                .allSatisfy(url -> assertThat(url)
                                        .startsWith("https://")
                                        .doesNotContain("/video", "english.chosun.com"));
                        assertThat(ReflectionTestUtils.getField(client, "restClient"))
                                .isSameAs(restClient);
                    }
                });
    }
}
