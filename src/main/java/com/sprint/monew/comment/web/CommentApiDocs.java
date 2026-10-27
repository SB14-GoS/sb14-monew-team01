package com.sprint.monew.comment.web;

import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.comment.web.dto.CommentRegisterRequest;
import com.sprint.monew.comment.web.dto.CommentUpdateRequest;
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
    ResponseEntity<CommentDto> register(CommentRegisterRequest request);

    @Operation(summary = "댓글 전체 읽기")
    ResponseEntity<CursorPageResponse<CommentDto>> readAll(
            UUID requesterId, UUID articleId, String orderBy, SortDirection direction, CursorRequest request);

    @Operation(summary = "댓글 수정")
    ResponseEntity<CommentDto> update(UUID commentId, UUID userId, CommentUpdateRequest request);

    @Operation(summary = "댓글 논리 삭제")
    void delete(UUID commentId, UUID requesterUserId);

    @Operation(summary = "댓글 물리 삭제")
    void deleteHard(UUID commentId, UUID requesterUserId);
}
