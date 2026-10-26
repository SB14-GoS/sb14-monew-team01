package com.sprint.monew.comment.web;

import com.sprint.monew.comment.application.CommentService;
import com.sprint.monew.comment.web.dto.CommentDto;
import com.sprint.monew.comment.web.dto.CommentRegisterRequest;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.common.SortDirection;
import com.sprint.monew.global.web.RequestUserId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApiDocs {
    private final CommentService commentService;

    @PostMapping
    @Override
    public ResponseEntity<CommentDto> register(@Valid @RequestBody CommentRegisterRequest request) {
        CommentDto commentDto = commentService.register(request.articleId(), request.userId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentDto);
    }

    @GetMapping
    @Override
    public ResponseEntity<CursorPageResponse<CommentDto>> readAll(
            @RequestUserId UUID requesterId,
            @RequestParam UUID articleId,
            @RequestParam String orderBy,
            @RequestParam SortDirection direction,
            @Valid @ModelAttribute CursorRequest request) {

        CursorPageResponse<CommentDto> response =
                commentService.search(articleId, requesterId, orderBy, direction, request);

        return ResponseEntity.ok(response);
    }
}
