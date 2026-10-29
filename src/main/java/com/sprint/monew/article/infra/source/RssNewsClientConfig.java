package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.domain.ArticleSource;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RssNewsClientConfig {

    @Bean
    public RestClient rssRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    public RssNewsClient hankyungNewsClient(
            @Value("${news.rss.hankyung-url}") String feedUrl, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.HANKYUNG, feedUrl, restClient);
    }

    @Bean
    public RssNewsClient chosunNewsClient(
            @Value("${news.rss.chosun-url}") String feedUrl, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.CHOSUN, feedUrl, restClient);
    }

    @Bean
    public RssNewsClient yeonhapNewsClient(
            @Value("${news.rss.yeonhap-url}") String feedUrl, @Qualifier("rssRestClient") RestClient restClient) {
        return new RssNewsClient(ArticleSource.YEONHAP, feedUrl, restClient);
    }
}
