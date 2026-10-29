package com.sprint.monew.article.infra.source.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/* 레코드에 없는 필드들은 무시 해버림 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RssChannel(
        @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "item")
        List<RssItem> items) {}
