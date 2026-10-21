package com.sprint.monew.article.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.article.api.ArticleViewedEvent;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleRepository;
import com.sprint.monew.article.domain.ArticleSource;
import com.sprint.monew.article.domain.ArticleView;
import com.sprint.monew.article.domain.ArticleViewRepository;
import com.sprint.monew.article.domain.exception.ArticleNotFoundException;
import com.sprint.monew.article.web.dto.ArticleViewDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleViewServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    ArticleViewRepository articleViewRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ArticleViewService articleViewService;

    private final UUID articleId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private static Article article() {
        return Article.create(
                "모뉴 기사", Instant.parse("2026-10-21T00:00:00Z"), "요약", ArticleSource.NAVER, "https://news.example/1");
    }

    @Test
    @DisplayName("기사가 없으면 ArticleNotFoundException을 던진다")
    void throwsWhenArticleNotFound() {
        given(articleRepository.findById(articleId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> articleViewService.register(articleId, userId))
                .isInstanceOf(ArticleNotFoundException.class);
        verifyNoInteractions(articleViewRepository, eventPublisher);
    }

    @Test
    @DisplayName("논리 삭제된 기사면 ArticleNotFoundException을 던진다")
    void throwsWhenArticleDeleted() {
        Article deleted = article();
        deleted.softDelete();
        given(articleRepository.findById(articleId)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> articleViewService.register(articleId, userId))
                .isInstanceOf(ArticleNotFoundException.class);
        verifyNoInteractions(articleViewRepository, eventPublisher);
    }

    @Test
    @DisplayName("처음 본 기사면 기록을 저장하고 조회수를 올린 뒤 이벤트를 발행한다")
    void registersFirstView() {
        Article article = article();
        Article updated = article();
        ReflectionTestUtils.setField(updated, "viewCount", 1L); // DB에서 증가된 뒤 다시 읽은 상태
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article), Optional.of(updated));
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.empty());
        given(articleViewRepository.save(any(ArticleView.class))).willAnswer(invocation -> invocation.getArgument(0));

        ArticleViewDto result = articleViewService.register(articleId, userId);

        verify(articleViewRepository).save(any(ArticleView.class));
        verify(articleRepository).increaseViewCount(articleId);
        ArgumentCaptor<ArticleViewedEvent> event = ArgumentCaptor.forClass(ArticleViewedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertThat(result.viewedBy()).isEqualTo(userId);
        assertThat(result.articleTitle()).isEqualTo("모뉴 기사");
        assertThat(result.source()).isEqualTo("NAVER");
        assertThat(result.articleViewCount()).isEqualTo(1L);
        assertThat(event.getValue().userId()).isEqualTo(userId);
        assertThat(event.getValue().articleViewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 본 기사면 기존 기록을 돌려주고 조회수와 이벤트는 그대로 둔다")
    void returnsExistingView() {
        Article article = article();
        ArticleView existing = ArticleView.create(article, userId);
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.of(existing));

        ArticleViewDto result = articleViewService.register(articleId, userId);

        verify(articleViewRepository, never()).save(any(ArticleView.class));
        verify(articleRepository, never()).increaseViewCount(any());
        verifyNoInteractions(eventPublisher);
        assertThat(result.viewedBy()).isEqualTo(userId);
        assertThat(result.articleViewCount()).isZero();
    }
}
