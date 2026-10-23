package com.sprint.monew.interest.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InterestUpdateRequest(
        @NotNull(message = "키워드 목록은 필수입니다.") @Size(min = 1, max = 10, message = "키워드는 1개 이상 10개 이하로 입력해주세요.")
        List<@NotBlank(message = "키워드는 공백일 수 없습니다.") String> keywords) {}
