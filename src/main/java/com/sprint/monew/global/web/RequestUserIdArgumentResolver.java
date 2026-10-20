package com.sprint.monew.global.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class RequestUserIdArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String USER_ID_HEADER = "Monew-Request-User-ID";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        // @RequestUserId 어노테이션이 있는지 확인
        boolean hasAnnotation = parameter.hasParameterAnnotation(RequestUserId.class);

        // UUID가 맞는지 확인
        boolean isRightType = parameter.getParameterType().equals(UUID.class);

        return hasAnnotation && isRightType;
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        // 요청 객체 가져오기
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // 인증 정보 추출
        String userIDStr = request.getHeader(USER_ID_HEADER);

        if (!StringUtils.hasText(userIDStr)) {
            throw new IllegalArgumentException("필수 요청 헤더 누락: " + USER_ID_HEADER);
        }

        try {
            return UUID.fromString(userIDStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 UUID 형식입니다: " + USER_ID_HEADER);
        }
    }
}
