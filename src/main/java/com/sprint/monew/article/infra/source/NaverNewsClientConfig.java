package com.sprint.monew.article.infra.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class NaverNewsClientConfig {

    @Bean
    public RestClient naverRestClient(
            @Value("${news.naver.client-id}") String clientId,
            @Value("${news.naver.client-secret}") String clientSecret) {

        /* 네이버는 JSON 본문을 text/plain으로 응답하기도 한다. 때문에 json으로 변경해주기 */
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        /* 요청 보낼 클라이언트 생성 */
        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://naverapihub.apigw.ntruss.com")
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .configureMessageConverters(converters -> converters.withJsonConverter(jsonConverter))
                .build();
    }

    @Bean
    public NaverNewsClient naverNewsClient(@Qualifier("naverRestClient") RestClient restClient) {
        return new NaverNewsClient(restClient);
    }
}
