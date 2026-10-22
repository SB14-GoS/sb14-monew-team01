package com.sprint.monew.article.application;

import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.exception.UserIdRequiredException;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.domain.repository.ArticleViewRepository;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleQueryRepository articleQueryRepository;
    private final ArticleViewRepository articleViewRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse<ArticleDto> search(ArticleSearchCondition condition, UUID userId) {
        if (userId == null) {
            throw new UserIdRequiredException();
        }
        /* UserNotFound 예외처리 구현 되어야함
          UserQueryApi.exists(userId) 유저 존재하는지 확인하는 쿼리구현 되어야함
        */

        /* 조건에 맞게 기사 가져옴 (limit + 1개 가져옴) */
        List<Article> articles = articleQueryRepository.search(condition);
        /* 조건에 맞는 기사의 총갯수 가져옴 */
        long totalElements = articleQueryRepository.countByCondition(condition);

        /* 위의 두개로 페이지 만들기 */
        CursorPageResponse<Article> page = CursorPageResponse.of(
                articles,
                condition.page().limit(),
                totalElements,
                article -> cursorOf(article, condition.orderBy()),
                Article::getCreatedAt);

        /* 페이지화된 기사들을 아아디 리스트로 변환 */
        List<UUID> articleIds = page.content().stream().map(Article::getId).toList();

        /* 입력받은(요청한) 사용자가 이미 읽은 기사인지 */
        Set<UUID> viewedArticleIds;
        if (articleIds.isEmpty()) {
            viewedArticleIds = Set.of();
        } else {
            viewedArticleIds = articleViewRepository.findViewedArticleIds(userId, articleIds);
        }

        /* 읽음여부 체크 */
        List<ArticleDto> content = page.content().stream()
                .map(article -> ArticleDto.from(
                        article,
                        /* 읽었으면 true 넘겨줘서 Dto.from(article,true) 로 넘겨줌
                        그리고 viewByMe true지정, 안읽었으면 false반환 안읽음으로 표시
                        contains()로 존재, 미존재 여부 확인
                         */
                        viewedArticleIds.contains(article.getId())))
                .toList();

        return new CursorPageResponse<>(
                content, page.nextCursor(), page.nextAfter(), page.size(), page.totalElements(), page.hasNext());
    }

    private String cursorOf(Article article, String orderBy) {
        return switch (orderBy) {
            case "publishDate" -> article.getPublishedDate().toString();
            case "commentCount" -> Long.toString(article.getCommentCount());
            case "viewCount" -> Long.toString(article.getViewCount());
            default -> throw new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("field", "orderBy"));
        };
    }
}
