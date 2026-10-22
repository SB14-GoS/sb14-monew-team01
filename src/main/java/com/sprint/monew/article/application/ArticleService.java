package com.sprint.monew.article.application;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.article.domain.Article;
import com.sprint.monew.article.domain.ArticleOrderBy;
import com.sprint.monew.article.domain.ArticleView;
import com.sprint.monew.article.domain.exception.ArticleNotFoundException;
import com.sprint.monew.article.domain.exception.UserIdRequiredException;
import com.sprint.monew.article.domain.repository.ArticleQueryRepository;
import com.sprint.monew.article.domain.repository.ArticleRepository;
import com.sprint.monew.article.domain.repository.ArticleSearchCondition;
import com.sprint.monew.article.domain.repository.ArticleSearchCursor;
import com.sprint.monew.article.domain.repository.ArticleViewRepository;
import com.sprint.monew.article.web.dto.ArticleDto;
import com.sprint.monew.global.common.CursorPageResponse;
import com.sprint.monew.global.common.CursorRequest;
import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService implements ArticleQueryApi, ArticleCommandApi {

    private final ArticleQueryRepository articleQueryRepository;
    private final ArticleViewRepository articleViewRepository;
    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public ArticleDto read(UUID articleId, UUID userId) {
        if (userId == null) {
            throw new UserIdRequiredException();
        }
        /* 조회했을때 없는얘량 논리삭제된얘 예외던지기 */
        Article article =
                articleRepository.findById(articleId).orElseThrow(() -> new ArticleNotFoundException(articleId));
        if (article.isDeleted()) {
            throw new ArticleNotFoundException(articleId);
        }

        /* 요청받은 유저가 읽었는지 안읽었는지 */
        Optional<ArticleView> articleView = articleViewRepository.findByArticleIdAndUserId(articleId, userId);
        boolean viewedByUser = articleView.isPresent();

        return ArticleDto.from(article, viewedByUser);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<ArticleDto> search(ArticleSearchCondition condition, UUID userId) {
        if (userId == null) {
            throw new UserIdRequiredException();
        }
        ArticleSearchCursor cursor = validateCondition(condition);
        /* UserNotFound 예외처리 구현 되어야함
          UserQueryApi.exists(userId) 유저 존재하는지 확인하는 쿼리구현 되어야함
        */

        /* 조건에 맞게 기사 가져옴 (limit + 1개 가져옴) */
        List<Article> articles = articleQueryRepository.search(condition, cursor);
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
        /* 읽었으면 true 넘겨줘서 Dto.from(article,true) 로 넘겨줌
        그리고 viewByMe true지정, 안읽었으면 false반환 안읽음으로 표시
        contains()로 존재, 미존재 여부 확인
         */
        List<ArticleDto> content = page.content().stream()
                .map(article -> ArticleDto.from(article, viewedArticleIds.contains(article.getId())))
                .toList();

        return new CursorPageResponse<>(
                content, page.nextCursor(), page.nextAfter(), page.size(), page.totalElements(), page.hasNext());
    }

    private ArticleSearchCursor validateCondition(ArticleSearchCondition condition) {
        if (condition.orderBy() == null) {
            throw invalidRequest("orderBy");
        }
        if (condition.direction() == null) {
            throw invalidRequest("direction");
        }
        CursorRequest page = condition.page();
        if (page == null || page.limit() == null || page.limit() < 1 || page.limit() > 100) {
            throw invalidRequest("limit");
        }
        if (condition.publishDateFrom() != null
                && condition.publishDateTo() != null
                && condition.publishDateFrom().isAfter(condition.publishDateTo())) {
            throw invalidRequest("publishDateFrom/publishDateTo");
        }

        boolean hasCursor = page.cursor() != null && !page.cursor().isBlank();
        if (!hasCursor && page.after() == null) {
            return null;
        }
        if (!hasCursor || page.after() == null) {
            throw invalidRequest("cursor/after");
        }
        try {
            String[] parts = page.cursor().split("\\|", -1);
            if (parts.length != 2) {
                throw invalidRequest("cursor");
            }
            UUID articleId = UUID.fromString(parts[1]);
            if (!articleId.toString().equalsIgnoreCase(parts[1])) {
                throw invalidRequest("cursor");
            }
            return switch (condition.orderBy()) {
                case PUBLISH_DATE -> new ArticleSearchCursor(Instant.parse(parts[0]), null, page.after(), articleId);
                case COMMENT_COUNT, VIEW_COUNT -> {
                    long count = Long.parseLong(parts[0]);
                    if (count < 0) {
                        throw invalidRequest("cursor");
                    }
                    yield new ArticleSearchCursor(null, count, page.after(), articleId);
                }
            };
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw invalidRequest("cursor");
        }
    }

    private MonewException invalidRequest(String field) {
        return new MonewException(CommonErrorCode.INCORRECT_REQUEST, Map.of("field", field));
    }

    private String cursorOf(Article article, ArticleOrderBy orderBy) {
        String value = switch (orderBy) {
            case PUBLISH_DATE -> article.getPublishedDate().toString();
            case COMMENT_COUNT -> Long.toString(article.getCommentCount());
            case VIEW_COUNT -> Long.toString(article.getViewCount());
        };
        return value + "|" + article.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(UUID articleId) {
        Optional<Article> target = articleRepository.findById(articleId);
        if (target.isEmpty()) {
            return false;
        }
        boolean result = target.get().isDeleted();

        return !result;
    }

    @Transactional
    @Override
    public void increaseCommentCount(UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new ArticleNotFoundException(articleId);
        }
        int updated = articleRepository.increaseCommentCount(articleId);
        if (updated == 0) {
            throw new ArticleNotFoundException(articleId);
        }
    }
}
