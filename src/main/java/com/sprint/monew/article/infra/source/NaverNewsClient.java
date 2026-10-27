package com.sprint.monew.article.infra.source;

import com.sprint.monew.article.application.port.NewsSourceClient;
import com.sprint.monew.article.application.port.RawArticle;
import com.sprint.monew.article.domain.ArticleSource;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverNewsClient implements NewsSourceClient {

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public NaverNewsClient(
            @Value("${news.naver.client-id}") String clientId,
            @Value("${news.naver.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        /* 요청 보낼 클라이언트 생성 */
        this.restClient = RestClient.builder()
                .baseUrl("https://naverapihub.apigw.ntruss.com")
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();
    }

    @Override
    public ArticleSource getSouce() {
        return ArticleSource.NAVER;
    }

    @Override
    public List<RawArticle> fetch(Collection<String> keywords) {
        return List.of();
    }
}
