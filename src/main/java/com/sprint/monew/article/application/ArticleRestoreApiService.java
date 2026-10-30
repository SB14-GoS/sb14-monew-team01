package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleRestoreApi;
import com.sprint.monew.article.api.ArticleRestoreCommand;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ArticleRestoreApiService implements ArticleRestoreApi {

    private final ArticleRepository articleRepository;

    @Override
    public Set<String> findExistingSourceUrls(List<String> sourceUrls) {
        if (sourceUrls.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(articleRepository.findSourceUrlsIn(sourceUrls));
    }

    @Override
    @Transactional
    public List<UUID> restore(List<ArticleRestoreCommand> commands) {
        List<Article> articles = commands.stream().map(this::toArticle).toList();
        return articleRepository.saveAll(articles).stream().map(Article::getId).toList();
    }

    private Article toArticle(ArticleRestoreCommand command) {
        return Article.create(
                command.title(),
                command.publishedDate(),
                command.summary(),
                ArticleSource.valueOf(command.source()),
                command.sourceUrl());
    }
}
