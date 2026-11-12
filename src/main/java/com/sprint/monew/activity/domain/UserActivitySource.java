package com.sprint.monew.activity.domain;

import com.sprint.monew.activity.domain.UserActivityDocument.ArticleViewItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentItem;
import com.sprint.monew.activity.domain.UserActivityDocument.CommentLikeItem;
import com.sprint.monew.activity.domain.UserActivityDocument.SubscriptionItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 원본(PostgreSQL)으로 다시 계산한 사용자 활동 내역. 정합성 보정의 기준이다. */
public record UserActivitySource(
        UUID userId,
        String email,
        String nickname,
        Instant createdAt,
        List<SubscriptionItem> subscriptions,
        List<CommentItem> comments,
        List<CommentLikeItem> commentLikes,
        List<ArticleViewItem> articleViews) {}
