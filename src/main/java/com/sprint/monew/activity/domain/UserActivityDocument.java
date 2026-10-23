package com.sprint.monew.activity.domain;

import com.sprint.monew.activity.web.dto.ArticleViewActivityDto;
import com.sprint.monew.activity.web.dto.CommentActivityDto;
import com.sprint.monew.activity.web.dto.CommentLikeActivityDto;
import com.sprint.monew.activity.web.dto.SubscriptionActivityDto;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collation = "user_activities")
public class UserActivityDocument {

    @Id
    UUID id;

    String email;
    String nickname;
    Instant createdAt;

    @Max(10)
    List<SubscriptionActivityDto> subscriptions;

    List<CommentActivityDto> comments;
    List<CommentLikeActivityDto> commentLikes;
    List<ArticleViewActivityDto> articleViews;
}
