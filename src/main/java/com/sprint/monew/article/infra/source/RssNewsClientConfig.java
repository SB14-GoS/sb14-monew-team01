package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.domain.ArticleSource;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RssProperties.class)
public class RssNewsClientConfig {

    @Bean
    public RestClient rssRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        // 원문 기록과 DTO 변환에서 응답 본문을 각각 읽는다.
        return RestClient.builder()
                .requestFactory(requestFactory)
                .bufferContent((uri, method) -> true)
                .build();
    }

    @Bean
    public RssNewsClient hankyungNewsClient(
            RssProperties properties, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.HANKYUNG, properties.hankyungUrls(), restClient);
    }

    @Bean
    public RssNewsClient chosunNewsClient(RssProperties properties, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.CHOSUN, properties.chosunUrls(), restClient);
    }

    @Bean
    public RssNewsClient yeonhapNewsClient(
            RssProperties properties, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.YEONHAP, properties.yeonhapUrls(), restClient);
    }
}
