package com.sprint.monew.article.infra.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.article.application.port.NewsSourceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class NaverNewsClientConfigTest {

    @Test
    void injectsNaverRestClientWhenNaverAndRssConfigurationsAreLoadedTogether() {
        new ApplicationContextRunner()
                .withUserConfiguration(NaverNewsClientConfig.class, RssNewsClientConfig.class)
                .withPropertyValues(
                        "news.naver.client-id=test-id",
                        "news.naver.client-secret=test-secret",
                        "news.rss.hankyung-url=https://rss.example/hankyung",
                        "news.rss.chosun-url=https://rss.example/chosun",
                        "news.rss.yeonhap-url=https://rss.example/yeonhap")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(NaverNewsClient.class);
                    assertThat(context.getBeansOfType(NewsSourceClient.class))
                            .containsOnlyKeys(
                                    "naverNewsClient", "hankyungNewsClient", "chosunNewsClient", "yeonhapNewsClient");

                    RestClient naverRestClient = context.getBean("naverRestClient", RestClient.class);
                    RestClient rssRestClient = context.getBean("rssRestClient", RestClient.class);
                    assertThat(naverRestClient).isNotSameAs(rssRestClient);
                    assertThat(ReflectionTestUtils.getField(context.getBean(NaverNewsClient.class), "restClient"))
                            .isSameAs(naverRestClient);
                    for (RssNewsClient client :
                            context.getBeansOfType(RssNewsClient.class).values()) {
                        assertThat(ReflectionTestUtils.getField(client, "restClient"))
                                .isSameAs(rssRestClient);
                    }
                });
    }
}
