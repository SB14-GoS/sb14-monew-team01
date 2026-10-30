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

        /* JSON 본문이 text/plain 또는 text/html로 전달되는 경우도 읽는다. */
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
        supportedMediaTypes.add(MediaType.TEXT_HTML);
        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        /* 요청 보낼 클라이언트 생성 */
        return RestClient.builder()
                .requestFactory(requestFactory)
                // 원문 기록과 DTO 변환에서 응답 본문을 각각 읽는다.
                .bufferContent((uri, method) -> true)
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
