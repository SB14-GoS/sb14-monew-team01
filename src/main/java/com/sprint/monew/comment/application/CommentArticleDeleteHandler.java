package com.sprint.monew.comment.application;

import com.sprint.monew.article.api.ArticleDeleteEvent;
import com.sprint.monew.comment.api.CommentCommandApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentArticleDeleteHandler {

    private final CommentCommandApi commentCommandApi;

    @EventListener
    public void onArticleDelete(ArticleDeleteEvent event) {
        commentCommandApi.deleteAllByArticleId(event.articleId());
    }
}
