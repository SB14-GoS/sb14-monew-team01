package com.sprint.monew.comment.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.monew.comment.domain.Comment;
import com.sprint.monew.comment.domain.CommentLike;
import com.sprint.monew.comment.domain.CommentOrderBy;
import com.sprint.monew.comment.domain.repository.CommentSearchResult;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentQueryRepositoryImplIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-10-26T00:00:00Z");

    @Autowired
    CommentQueryRepositoryImpl commentQueryRepository;

    @Autowired
    EntityManager entityManager;

    static Stream<Arguments> orderByAndDirections() {
        return Stream.of(CommentOrderBy.CREATED_AT, CommentOrderBy.LIKE_COUNT)
                .flatMap(orderBy -> Stream.of(SortDirection.ASC, SortDirection.DESC)
                        .map(direction -> Arguments.of(orderBy, direction)));
    }

    @ParameterizedTest
    @MethodSource("orderByAndDirections")
    void 동점이_있어도_모든_정렬에서_커서_페이지를_끝까지_넘기면_누락과_중복이_없다(CommentOrderBy orderBy, SortDirection direction) {
        UUID articleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        List<Comment> comments = List.of(
                saveComment(articleId, BASE_TIME.plusSeconds(1), 1L),
                saveComment(articleId, BASE_TIME.plusSeconds(2), 2L),
                saveComment(articleId, BASE_TIME.plusSeconds(2), 2L),
                saveComment(articleId, BASE_TIME.plusSeconds(3), 3L),
                saveComment(articleId, BASE_TIME.plusSeconds(4), 4L));

        List<CommentSearchResult> actual = findAllPages(articleId, requesterId, orderBy, direction, 2);

        assertThat(actual)
                .extracting(result -> result.comment().getId())
                .containsExactlyInAnyOrderElementsOf(
                        comments.stream().map(Comment::getId).toList())
                .doesNotHaveDuplicates();
        assertSortedByRequestedValue(actual, orderBy, direction);
    }

    @ParameterizedTest
    @MethodSource("orderByAndDirections")
    void 요청자가_좋아요한_댓글에만_likedByMe가_true이고_다른_사용자의_좋아요는_영향이_없다(CommentOrderBy orderBy, SortDirection direction) {
        UUID articleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Comment likedByRequester = saveComment(articleId, BASE_TIME.plusSeconds(1), 2L);
        Comment likedByOtherUser = saveComment(articleId, BASE_TIME.plusSeconds(2), 1L);
        Comment notLiked = saveComment(articleId, BASE_TIME.plusSeconds(3), 0L);

        saveLike(likedByRequester, requesterId);
        saveLike(likedByRequester, UUID.randomUUID());
        saveLike(likedByOtherUser, UUID.randomUUID());

        List<CommentSearchResult> results = findAllPages(articleId, requesterId, orderBy, direction, 2);

        assertThat(results)
                .filteredOn(result -> result.comment().getId().equals(likedByRequester.getId()))
                .extracting(CommentSearchResult::likedByMe)
                .containsExactly(true);
        assertThat(results)
                .filteredOn(result -> result.comment().getId().equals(likedByOtherUser.getId()))
                .extracting(CommentSearchResult::likedByMe)
                .containsExactly(false);
        assertThat(results)
                .filteredOn(result -> result.comment().getId().equals(notLiked.getId()))
                .extracting(CommentSearchResult::likedByMe)
                .containsExactly(false);
    }

    @ParameterizedTest
    @MethodSource("orderByAndDirections")
    void 소프트_삭제_댓글과_다른_기사의_댓글은_목록과_totalElements에서_제외된다(CommentOrderBy orderBy, SortDirection direction) {
        UUID targetArticleId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Comment first = saveComment(targetArticleId, BASE_TIME.plusSeconds(1), 1L);
        Comment second = saveComment(targetArticleId, BASE_TIME.plusSeconds(2), 2L);
        Comment deleted = saveComment(targetArticleId, BASE_TIME.plusSeconds(3), 3L);
        Comment otherArticleComment = saveComment(UUID.randomUUID(), BASE_TIME.plusSeconds(4), 4L);

        Comment managedDeleted = entityManager.find(Comment.class, deleted.getId());
        managedDeleted.softDelete();
        entityManager.flush();
        entityManager.clear();

        List<CommentSearchResult> results = findAllPages(targetArticleId, requesterId, orderBy, direction, 2);

        assertThat(results)
                .extracting(result -> result.comment().getId())
                .containsExactlyInAnyOrder(first.getId(), second.getId())
                .doesNotContain(deleted.getId(), otherArticleComment.getId());
        assertThat(commentQueryRepository.countByArticleId(targetArticleId)).isEqualTo(2L);
    }

    private List<CommentSearchResult> findAllPages(
            UUID articleId, UUID requesterId, CommentOrderBy orderBy, SortDirection direction, int limit) {
        List<CommentSearchResult> all = new ArrayList<>();
        String cursor = null;
        Instant after = null;

        for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
            CursorRequest request = new CursorRequest(cursor, after, limit);
            List<CommentSearchResult> fetched =
                    commentQueryRepository.findByArticleId(articleId, requesterId, orderBy, direction, request);
            CursorPageResponse<CommentSearchResult> page = CursorPageResponse.of(
                    fetched,
                    limit,
                    commentQueryRepository.countByArticleId(articleId),
                    result -> cursorOf(result, orderBy),
                    result -> result.comment().getCreatedAt());

            all.addAll(page.content());
            if (!page.hasNext()) {
                return all;
            }

            cursor = page.nextCursor();
            after = page.nextAfter();
        }

        throw new AssertionError("커서 페이지네이션이 종료되지 않았습니다.");
    }

    private String cursorOf(CommentSearchResult result, CommentOrderBy orderBy) {
        Comment comment = result.comment();

        return switch (orderBy) {
            case CREATED_AT -> comment.getId().toString();
            case LIKE_COUNT -> comment.getLikeCount() + "_" + comment.getId();
        };
    }

    private Comment saveComment(UUID articleId, Instant createdAt, long likeCount) {
        Comment comment = new Comment(articleId, UUID.randomUUID(), "댓글 내용");
        entityManager.persist(comment);
        entityManager.flush();

        entityManager
                .createQuery("update Comment c set c.createdAt = :createdAt, c.likeCount = :likeCount where c.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("likeCount", likeCount)
                .setParameter("id", comment.getId())
                .executeUpdate();
        entityManager.clear();

        return entityManager.find(Comment.class, comment.getId());
    }

    private void saveLike(Comment comment, UUID userId) {
        Comment managedComment = entityManager.getReference(Comment.class, comment.getId());
        entityManager.persist(new CommentLike(managedComment, userId, managedComment.getArticleId()));
        entityManager.flush();
        entityManager.clear();
    }

    private void assertSortedByRequestedValue(
            List<CommentSearchResult> results, CommentOrderBy orderBy, SortDirection direction) {
        for (int index = 1; index < results.size(); index++) {
            Comment previous = results.get(index - 1).comment();
            Comment current = results.get(index).comment();

            int comparison = switch (orderBy) {
                case CREATED_AT -> current.getCreatedAt().compareTo(previous.getCreatedAt());
                case LIKE_COUNT -> Long.compare(current.getLikeCount(), previous.getLikeCount());
            };

            if (direction == SortDirection.ASC) {
                assertThat(comparison).isGreaterThanOrEqualTo(0);
            } else {
                assertThat(comparison).isLessThanOrEqualTo(0);
            }
        }
    }
}
