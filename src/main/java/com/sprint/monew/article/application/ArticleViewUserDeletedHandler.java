package com.sprint.monew.article.application;

import com.sprint.monew.article.domain.repository.ArticleViewRepository;
import com.sprint.monew.user.api.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ArticleViewUserDeletedHandler {

    private final ArticleViewRepository articleViewRepository;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        articleViewRepository.deleteAllByUserId(event.userId());
    }
}
