package com.sprint.monew.global.config;

import com.sprint.monew.global.web.RequestTraceConstants;
import com.sprint.monew.global.web.RequestUserId;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        // @RequestUserId 파라미터는 문서에서 숨기고, 아래 customizer가 요청 헤더로 대신 표시한다
        SpringDocUtils.getConfig().addAnnotationsToIgnore(RequestUserId.class);
    }

    @Bean
    public OpenAPI monewOpenApi() {
        return new OpenAPI()
                .info(new Info().title("모뉴 API").version("v1").description("다중 뉴스 API 통합 맞춤형 뉴스 & 소셜 서비스"))
                .tags(List.of(
                        new Tag().name("사용자 관리").description("사용자 관련 API"),
                        new Tag().name("관심사 관리").description("관심사 관련 API"),
                        new Tag().name("뉴스 기사 관리").description("뉴스 기사 관련 API"),
                        new Tag().name("댓글 관리").description("댓글 관련 API"),
                        new Tag().name("알림 관리").description("알림 관련 API"),
                        new Tag().name("사용자 활동 내역 관리").description("사용자 활동 내역 관련 API")));
    }

    @Bean
    public OperationCustomizer requestUserIdHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean usesRequestUserId = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(RequestUserId.class));

            if (usesRequestUserId) {
                operation.addParametersItem(new HeaderParameter()
                        .name(RequestTraceConstants.USER_ID_HEADER)
                        .description("요청자 ID")
                        .required(true)
                        .schema(new UUIDSchema()));
            }
            return operation;
        };
    }
}
