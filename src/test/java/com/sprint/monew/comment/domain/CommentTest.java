package com.sprint.monew.comment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.monew.comment.domain.exception.ArticleIdRequiredException;
import com.sprint.monew.comment.domain.exception.CommentUserIdRequiredException;
import com.sprint.monew.comment.domain.exception.InvalidCommentContentException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommentTest {

    private final UUID articleId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void 유효한_댓글을_생성할_수_있다() {
        assertThatCode(() -> new Comment(articleId, userId, "a".repeat(500))).doesNotThrowAnyException();
    }

    @Test
    void articleId가_null이면_댓글을_생성할_수_없다() {
        assertThatThrownBy(() -> new Comment(null, userId, "댓글")).isInstanceOf(ArticleIdRequiredException.class);
    }

    @Test
    void userId가_null이면_댓글을_생성할_수_없다() {
        assertThatThrownBy(() -> new Comment(articleId, null, "댓글")).isInstanceOf(CommentUserIdRequiredException.class);
    }

    @Test
    void null_공백_또는_501자_내용으로_댓글을_생성할_수_없다() {
        assertThatThrownBy(() -> new Comment(articleId, userId, null))
                .isInstanceOf(InvalidCommentContentException.class);
        assertThatThrownBy(() -> new Comment(articleId, userId, "  \t\n"))
                .isInstanceOf(InvalidCommentContentException.class);
        assertThatThrownBy(() -> new Comment(articleId, userId, "a".repeat(501)))
                .isInstanceOf(InvalidCommentContentException.class);
    }
}
