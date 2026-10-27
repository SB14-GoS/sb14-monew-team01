package com.sprint.monew.article.api;

import java.time.Instant;
import java.util.List;

public interface ArticleBackupApi {
    // from 포함, to 미포함 -> 그 기간 내의 기사
    List<ArticleBackupDto> findAllCreatedBetween(Instant from, Instant to);
}
