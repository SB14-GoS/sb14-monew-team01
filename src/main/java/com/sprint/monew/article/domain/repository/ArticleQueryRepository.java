package com.sprint.monew.article.domain.repository;

import com.sprint.monew.article.domain.Article;
import java.util.List;

public interface ArticleQueryRepository {
    List<Article> search(ArticleSearchCondition conditoin);

    long countByCondition(ArticleSearchCondition condition);
}
