package com.sprint.monew.article.infra.source.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/* 레코드에 없는 필드들은 무시 해버림 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RssResponse(RssChannel channel) {}
