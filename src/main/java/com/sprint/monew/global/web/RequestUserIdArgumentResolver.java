package com.sprint.monew.global.web;

import static com.sprint.monew.global.web.RequestTraceConstants.USER_ID_HEADER;

import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class RequestUserIdArgumentResolver implements HandlerMethodArgumentResolver {

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
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {
        // 인증 정보 추출
        String userIdValue = webRequest.getHeader(USER_ID_HEADER);

        if (!StringUtils.hasText(userIdValue)) {
            throw new MissingRequestHeaderException(USER_ID_HEADER, parameter);
        }

        try {
            return UUID.fromString(userIdValue);
        } catch (IllegalArgumentException e) {
            throw new MethodArgumentTypeMismatchException(userIdValue, UUID.class, USER_ID_HEADER, parameter, e);
        }
    }
}
