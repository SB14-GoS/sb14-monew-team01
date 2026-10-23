package com.sprint.monew.comment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentRegisterRequest(
        @NotNull UUID articleId,
        UUID userId,

        @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.") @Size(max = 500, message = "댓글은 500자 이하여야 합니다.")
        String content) {}
