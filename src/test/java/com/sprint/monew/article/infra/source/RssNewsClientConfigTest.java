package com.sprint.monew.article.infra.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.domain.ArticleSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class RssNewsClientConfigTest {

    @Test
    void registersEachSourceWithItsConfiguredUrlAndSharedRestClient() {
        Map<ArticleSource, String> expectedUrls = Map.of(
                ArticleSource.HANKYUNG, "https://www.hankyung.com/feed/all-news",
                ArticleSource.CHOSUN, "https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml",
                ArticleSource.YEONHAP, "https://www.yna.co.kr/rss/news.xml");

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
                            .containsExactlyInAnyOrderElementsOf(expectedUrls.keySet());
                    RestClient restClient = context.getBean("rssRestClient", RestClient.class);
                    for (NewsSourceClient client : clients.values()) {
                        assertThat(ReflectionTestUtils.getField(client, "feedUrl"))
                                .isEqualTo(expectedUrls.get(client.getSource()));
                        assertThat(ReflectionTestUtils.getField(client, "restClient"))
                                .isSameAs(restClient);
                    }
                });
    }
}
