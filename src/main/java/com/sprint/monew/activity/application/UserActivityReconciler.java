package com.sprint.monew.activity.application;

import com.sprint.monew.activity.domain.UserActivityDocument;
import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import com.sprint.monew.activity.domain.UserActivityRepository;
import com.sprint.monew.activity.domain.UserActivitySource;
import com.sprint.monew.activity.domain.UserActivitySourceReader;
import com.sprint.monew.activity.domain.UserActivityWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * 원본(PostgreSQL)과 활동 내역(MongoDB)을 비교해 빠진 항목을 넣고 남은 항목을 지운다.
 *
 * <ul>
 *   <li>항목은 id 집합으로만 비교한다. 이미 있는 항목의 숫자(조회수·좋아요 수 등)는 활동 시점 스냅샷이므로 원본 현재 값으로 덮지 않는다
 *   <li>문서를 통째로 바꾸지 않고 relay와 같은 멱등 연산(add는 $ne 조건, remove는 $pull)만 쓴다. relay와 동시에 돌아도 서로의 반영을 지우지 않는다
 *   <li>MongoDB를 먼저 읽고 원본을 나중에 읽는다. 그래야 MongoDB에 있는 항목은 원본에도 이미 커밋돼 있어, 아직 relay 전인 항목을 잘못 지우지 않는다
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityReconciler {

    static final int CHUNK_SIZE = 500;
    private static final UUID MIN_ID = new UUID(0L, 0L);

    private final UserActivitySourceReader sourceReader;
    private final UserActivityRepository repository;
    private final UserActivityWriter writer;

    /** 사용자 전체를 묶음 단위로 보정하고, 사용자가 사라진 문서를 지운다. */
    public ReconcileResult reconcileAll() {
        ReconcileResult total = ReconcileResult.empty();
        UUID after = MIN_ID;
        while (true) {
            List<UUID> userIds = sourceReader.findUserIdsAfter(after, CHUNK_SIZE);
            if (userIds.isEmpty()) {
                break;
            }
            total = total.plus(reconcile(userIds));
            after = userIds.get(userIds.size() - 1);
        }
        total = total.plus(removeOrphans());
        log.info("활동 내역 정합성 보정 완료 - {}", total);
        return total;
    }

    public ReconcileResult reconcile(List<UUID> userIds) {
        Map<UUID, UserActivityDocument> documents = repository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserActivityDocument::getId, Function.identity()));
        Map<UUID, UserActivitySource> sources = sourceReader.load(userIds);

        ReconcileResult result = ReconcileResult.empty();
        for (UUID userId : userIds) {
            UserActivitySource source = sources.get(userId);
            if (source == null) {
                continue; // 그 사이 물리 삭제됨. 고아 문서 정리에서 지운다
            }
            result = result.plus(reconcileUser(source, documents.get(userId)));
        }
        return result;
    }

    private ReconcileResult reconcileUser(UserActivitySource source, UserActivityDocument document) {
        UUID userId = source.userId();
        if (document == null) {
            // 문서가 없으면 항목을 하나씩 넣지 않고 한 번에 넣는다. 그 사이 relay가 먼저 만들었으면 아래 비교로 메운다
            if (writer.restore(source)) {
                int items = source.subscriptions().size()
                        + source.comments().size()
                        + source.commentLikes().size()
                        + source.articleViews().size();
                return new ReconcileResult(1, 1, 1, items, 0, 0);
            }
            document = repository.findById(userId).orElse(null);
        }
        Changes changes = new Changes();
        if (document != null && !Objects.equals(document.getNickname(), source.nickname())) {
            writer.updateNickname(userId, source.nickname());
            changes.fields++;
        }

        diff(
                changes,
                userId,
                source.subscriptions(),
                document == null ? List.of() : document.getSubscriptions(),
                SubscriptionItem::interestId,
                writer::addSubscription,
                writer::removeSubscription);
        diff(
                changes,
                userId,
                source.comments(),
                document == null ? List.of() : document.getComments(),
                CommentItem::commentId,
                writer::addComment,
                writer::removeComment);
        // 좋아요는 Writer가 commentId로 지우므로 commentId로 비교한다 (사용자당 댓글 하나에 좋아요 하나)
        diff(
                changes,
                userId,
                source.commentLikes(),
                document == null ? List.of() : document.getCommentLikes(),
                CommentLikeItem::commentId,
                writer::addCommentLike,
                writer::removeCommentLike);
        diff(
                changes,
                userId,
                source.articleViews(),
                document == null ? List.of() : document.getArticleViews(),
                ArticleViewItem::articleViewId,
                writer::addArticleView,
                writer::removeArticleView);

        return new ReconcileResult(1, changes.any() ? 1 : 0, 0, changes.added, changes.removed, 0);
    }

    /** 남는 항목을 먼저 지우고 빠진 항목을 넣는다 (넣다가 최근 10개 제한에 정상 항목이 밀려나지 않게). */
    private static <T> void diff(
            Changes changes,
            UUID userId,
            List<T> expected,
            List<T> actual,
            Function<T, UUID> idOf,
            BiConsumer<UUID, T> add,
            BiConsumer<UUID, UUID> remove) {
        Set<UUID> expectedIds = expected.stream().map(idOf).collect(Collectors.toSet());
        Set<UUID> actualIds = actual.stream().map(idOf).collect(Collectors.toSet());
        for (UUID id : actualIds) {
            if (!expectedIds.contains(id)) {
                remove.accept(userId, id);
                changes.removed++;
            }
        }
        for (T item : expected) {
            if (!actualIds.contains(idOf.apply(item))) {
                add.accept(userId, item);
                changes.added++;
            }
        }
    }

    private ReconcileResult removeOrphans() {
        int removed = 0;
        UUID after = MIN_ID;
        while (true) {
            List<UUID> ids = repository.findIdsAfter(after, Limit.of(CHUNK_SIZE)).stream()
                    .map(UserActivityDocument::getId)
                    .toList();
            if (ids.isEmpty()) {
                break;
            }
            Set<UUID> existing = sourceReader.findExistingUserIds(ids);
            for (UUID id : ids) {
                if (!existing.contains(id)) {
                    writer.delete(id);
                    removed++;
                }
            }
            after = ids.get(ids.size() - 1);
        }
        return new ReconcileResult(0, 0, 0, 0, 0, removed);
    }

    private static final class Changes {
        private int added;
        private int removed;
        private int fields;

        private boolean any() {
            return added > 0 || removed > 0 || fields > 0;
        }
    }

    /**
     * @param scanned 검사한 사용자 수
     * @param repaired 하나라도 고친 문서 수
     * @param created 없어서 새로 만든 문서 수
     * @param itemsAdded 넣은 항목 수
     * @param itemsRemoved 지운 항목 수
     * @param orphansRemoved 사용자가 없어 지운 문서 수
     */
    public record ReconcileResult(
            int scanned, int repaired, int created, int itemsAdded, int itemsRemoved, int orphansRemoved) {

        static ReconcileResult empty() {
            return new ReconcileResult(0, 0, 0, 0, 0, 0);
        }

        ReconcileResult plus(ReconcileResult other) {
            return new ReconcileResult(
                    scanned + other.scanned,
                    repaired + other.repaired,
                    created + other.created,
                    itemsAdded + other.itemsAdded,
                    itemsRemoved + other.itemsRemoved,
                    orphansRemoved + other.orphansRemoved);
        }
    }
}
