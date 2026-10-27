package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleBackupApi;
import com.sprint.monew.article.api.ArticleBackupDto;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleBackupApiService implements ArticleBackupApi {

    private final ArticleRepository articleRepository;

    @Override
    public List<ArticleBackupDto> findAllCreatedBetween(Instant from, Instant to) {
        return articleRepository.findByCreatedAtRange(from, to).stream()
                .map(this::toDto)
                .toList();
    }

    private ArticleBackupDto toDto(Article article) {
        return new ArticleBackupDto(
                article.getId(),
                article.getTitle(),
                article.getPublishedDate(),
                article.getSummary(),
                article.getSource().name(),
                article.getSourceUrl());
    }
}
