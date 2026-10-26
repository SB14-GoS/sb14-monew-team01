package com.sprint.monew.comment.application;

import com.sprint.monew.comment.api.CommentCommandApi;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommentCleanupService implements CommentCommandApi {
    @Override
    public void deleteAllByArticleId(UUID articleId) {
        /* 여기에 댓글을 물리삭제 하는 로직을 구현해주세요
          반드시 꼮꼮 물리삭제 로직을 만들어 주셔야 합니다
          작성 하시고 해당 주석은 지워주세요 감사합니다~
        */
    }
}
