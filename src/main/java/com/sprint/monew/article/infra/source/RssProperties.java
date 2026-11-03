package com.sprint.monew.article.infra.source;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("news.rss")
public record RssProperties(
        @NotEmpty List<String> hankyungUrls,
        @NotEmpty List<String> chosunUrls,
        @NotEmpty List<String> yeonhapUrls) {}
