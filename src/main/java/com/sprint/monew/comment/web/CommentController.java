package com.sprint.monew.comment.web;

import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.comment.web.dto.CommentRegisterRequest;
import com.sprint.monew.global.web.RequestUserId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController implements CommentApiDocs {
    private final CommentService commentService;

    @PostMapping("api/comments")
    public ResponseEntity<CommentDto> register(
            @RequestUserId UUID userId, @Valid @RequestBody CommentRegisterRequest request) {
        CommentDto commentDto = commentService.register(request.articleId(), userId, request.content());
        return ResponseEntity.ok(commentDto);
    }
}
