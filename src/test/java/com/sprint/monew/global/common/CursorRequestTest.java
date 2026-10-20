package com.sprint.monew.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("첫 페이지 요청은 cursor와 after 없이 limit만으로 유효하다")
    void firstPage() {
        CursorRequest request = new CursorRequest(null, null, 10);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("다음 페이지 요청은 cursor와 after를 함께 담아 유효하다")
    void nextPage() {
        CursorRequest request = new CursorRequest("item4", Instant.parse("2026-10-20T00:00:04Z"), 10);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("limit이 없으면 검증에 실패한다")
    void limitNull() {
        CursorRequest request = new CursorRequest(null, null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("limit");
    }

    @Test
    @DisplayName("limit이 1보다 작으면 검증에 실패한다")
    void limitZero() {
        CursorRequest request = new CursorRequest(null, null, 0);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("limit");
    }
}
