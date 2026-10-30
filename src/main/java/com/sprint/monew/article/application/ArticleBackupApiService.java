package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleBackupApi;
import com.sprint.monew.article.api.ArticleBackupDto;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.repository.ArticleInterestIdPair;
import com.sprint.monew.article.domain.repository.ArticleInterestRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleBackupApiService implements ArticleBackupApi {

    private final ArticleRepository articleRepository;
    private final ArticleInterestRepository articleInterestRepository;

    @Override
    public List<ArticleBackupDto> findAllCreatedBetween(Instant from, Instant to) {
        List<Article> articles = articleRepository.findByCreatedAtRange(from, to);
        if (articles.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<UUID>> interestIdsByArticleId = groupInterestIdsByArticleId(articles);

        return articles.stream()
                .map(article -> toDto(article, interestIdsByArticleId.getOrDefault(article.getId(), List.of())))
                .toList();
    }

    private Map<UUID, List<UUID>> groupInterestIdsByArticleId(List<Article> articles) {
        List<UUID> articleIds = articles.stream().map(Article::getId).toList();
        List<ArticleInterestIdPair> pairs = articleInterestRepository.findInterestIdPairsByArticleIds(articleIds);
        // 기사 id별로 관심사 id를 묶기
        return pairs.stream()
                .collect(Collectors.groupingBy(
                        ArticleInterestIdPair::articleId,
                        Collectors.mapping(ArticleInterestIdPair::interestId, Collectors.toList())));
    }

    private ArticleBackupDto toDto(Article article, List<UUID> interestIds) {
        return new ArticleBackupDto(
                article.getId(),
                interestIds,
                article.getTitle(),
                article.getPublishedDate(),
                article.getSummary(),
                article.getSource().name(),
                article.getSourceUrl());
    }
}
