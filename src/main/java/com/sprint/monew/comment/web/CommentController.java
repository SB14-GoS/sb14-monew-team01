package com.sprint.monew.comment.web;

import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.comment.web.dto.CommentRegisterRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.web.RequestUserId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController implements CommentApiDocs {
    private final CommentService commentService;

    @PostMapping("api/comments")
    public ResponseEntity<CommentDto> register(
            @PathVariable UUID articleId,
            @RequestUserId UUID userId,
            @Valid @RequestBody CommentRegisterRequest request) {
        CommentDto commentDto = commentService.register(articleId, userId, request.content());
        return ResponseEntity.ok(commentDto);
    }

    @GetMapping("api/comments")
    public ResponseEntity<CursorPageResponse<CommentDto>> readAll(
            @PathVariable UUID articleId, @Valid @RequestBody CursorRequest request) {
        CursorPageResponse<CommentDto> readAll = commentService.readAll(articleId, request);
        return ResponseEntity.ok(readAll);
    }
}
