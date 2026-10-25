package com.sprint.monew.comment.web;

import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.comment.web.dto.CommentRegisterRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "댓글 관리")
public interface CommentApiDocs {

    @Operation(summary = "댓글 등록")
    ResponseEntity<CommentDto> register(UUID userId, CommentRegisterRequest request);

    @Operation(summary = "댓글 전체 읽기")
    ResponseEntity<CursorPageResponse<CommentDto>> readAll(
            UUID requestId, UUID articleId, String commentOrderBy, SortDirection direction, CursorRequest request);
}
