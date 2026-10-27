package com.sprint.monew.article.application.port;

import com.sprint.monew.article.domain.ArticleSource;
import java.util.Collection;
import java.util.List;

public interface NewsSourceClient {
    ArticleSource getSouce();

    List<RawArticle> fetch(Collection<String> keywords);
}
