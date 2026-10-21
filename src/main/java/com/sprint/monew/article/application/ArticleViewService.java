package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleRepository;
import com.sprint.monew.article.domain.ArticleView;
import com.sprint.monew.article.domain.ArticleViewRepository;
import com.sprint.monew.article.domain.exception.ArticleNotFoundException;
import com.sprint.monew.article.web.dto.ArticleViewDto;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleViewService {

    private final ArticleRepository articleRepository;
    private final ArticleViewRepository articleViewRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ArticleViewDto register(UUID articleId, UUID userId) {
        Article article = articleRepository
                .findById(articleId)
                .filter(a -> !a.isDeleted())
                .orElseThrow(() -> new ArticleNotFoundException(articleId));

        Optional<ArticleView> existing = articleViewRepository.findByArticleIdAndUserId(articleId, userId);
        if (existing.isPresent()) {
            return ArticleViewDto.from(existing.get(), article, article.getViewCount());
        }

        ArticleView articleView = articleViewRepository.save(ArticleView.create(article, userId));
        articleRepository.increaseViewCount(articleId);
        long viewCount = article.getViewCount() + 1;

        eventPublisher.publishEvent(toEvent(articleView, article, viewCount));
        return ArticleViewDto.from(articleView, article, viewCount);
    }

    private ArticleViewedEvent toEvent(ArticleView view, Article article, long viewCount) {
        return new ArticleViewedEvent(
                view.getId(),
                view.getUserId(),
                view.getCreatedAt(),
                article.getId(),
                article.getSource().name(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishedDate(),
                article.getSummary(),
                article.getCommentCount(),
                viewCount);
    }
}
