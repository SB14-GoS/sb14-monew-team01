package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleRestoreApi;
import com.sprint.monew.article.api.ArticleRestoreCommand;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleInterest;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.repository.ArticleInterestRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.interest.api.InterestQueryApi;
import java.util.ArrayList;
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
    private final ArticleInterestRepository articleInterestRepository;
    private final InterestQueryApi interestQueryApi;

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
        List<UUID> allInterestIds = commands.stream()
                .flatMap(command -> command.interestIds().stream())
                .distinct()
                .toList();
        Set<UUID> existing = interestQueryApi.findExistingIds(allInterestIds);

        List<Article> articles = commands.stream().map(this::toArticle).toList();
        List<Article> saved = articleRepository.saveAll(articles);

        List<ArticleInterest> articleInterests = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            Article article = saved.get(i);
            for (UUID interestId :
                    commands.get(i).interestIds().stream().distinct().toList()) {
                if (existing.contains(interestId)) {
                    articleInterests.add(ArticleInterest.create(article, interestId));
                }
            }
        }
        articleInterestRepository.saveAll(articleInterests);

        return saved.stream().map(Article::getId).toList();
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
