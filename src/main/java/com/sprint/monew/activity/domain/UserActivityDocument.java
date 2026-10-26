package com.sprint.monew.activity.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "user_activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityDocument {

    @Id
    private UUID id;

    private String email;
    private String nickname;
    private Instant createdAt;
    private List<SubscriptionItem> subscriptions = new ArrayList<>();
    private List<CommentItem> comments = new ArrayList<>();
    private List<CommentLikeItem> commentLikes = new ArrayList<>();
    private List<ArticleViewItem> articleViews = new ArrayList<>();

    public UserActivityDocument(UUID id, String email, String nickname, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public record SubscriptionItem(
            UUID subscriptionId,
            UUID interestId,
            String interestName,
            List<String> interestKeywords,
            long interestSubscriberCount,
            Instant createdAt) {}

    public record CommentItem(
            UUID commentId,
            UUID articleId,
            String articleTitle,
            UUID userId,
            String userNickname,
            String content,
            long likeCount,
            Instant createdAt) {}

    public record CommentLikeItem(
            UUID commentLikeId,
            Instant createdAt,
            UUID commentId,
            UUID articleId,
            String articleTitle,
            UUID commentUserId,
            String commentUserNickname,
            String commentContent,
            long commentLikeCount,
            Instant commentCreatedAt) {}

    public record ArticleViewItem(
            UUID articleViewId,
            UUID viewedBy,
            Instant createdAt,
            UUID articleId,
            String source,
            String sourceUrl,
            String articleTitle,
            Instant articlePublishedDate,
            String articleSummary,
            long articleCommentCount,
            long articleViewCount) {}
}
