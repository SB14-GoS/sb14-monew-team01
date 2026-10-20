package com.sprint.monew.global.web;

import static com.sprint.monew.global.web.RequestTraceConstants.CLIENT_IP_HEADER;
import static com.sprint.monew.global.web.RequestTraceConstants.MDC_CLIENT_IP;
import static com.sprint.monew.global.web.RequestTraceConstants.MDC_REQUEST_ID;
import static com.sprint.monew.global.web.RequestTraceConstants.REQUEST_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/articles");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }

    @Test
    @DisplayName("요청 처리 중 MDC에 32자리 요청 ID와 클라이언트 IP가 들어 있다")
    void putsMdcDuringRequest() throws Exception {
        Map<String, String> captured = new HashMap<>();
        FilterChain chain = (req, res) -> {
            captured.put(MDC_REQUEST_ID, MDC.get(MDC_REQUEST_ID));
            captured.put(MDC_CLIENT_IP, MDC.get(MDC_CLIENT_IP));
        };

        filter.doFilter(request(), new MockHttpServletResponse(), chain);

        assertThat(captured.get(MDC_REQUEST_ID)).matches("[0-9a-f]{32}");
        assertThat(captured.get(MDC_CLIENT_IP)).isEqualTo("192.0.2.10");
    }

    @Test
    @DisplayName("응답 헤더에 MDC와 같은 요청 ID와 클라이언트 IP를 담는다")
    void setsResponseHeaders() throws Exception {
        Map<String, String> captured = new HashMap<>();
        FilterChain chain = (req, res) -> captured.put(MDC_REQUEST_ID, MDC.get(MDC_REQUEST_ID));
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo(captured.get(MDC_REQUEST_ID));
        assertThat(response.getHeader(CLIENT_IP_HEADER)).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("요청마다 다른 요청 ID를 만든다")
    void differentRequestIdPerRequest() throws Exception {
        FilterChain chain = (req, res) -> {};
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        filter.doFilter(request(), first, chain);
        filter.doFilter(request(), second, chain);

        assertThat(first.getHeader(REQUEST_ID_HEADER)).isNotEqualTo(second.getHeader(REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("요청 처리가 끝나면 MDC를 비운다")
    void clearsMdcAfterRequest() throws Exception {
        filter.doFilter(request(), new MockHttpServletResponse(), (req, res) -> {});

        assertThat(MDC.get(MDC_REQUEST_ID)).isNull();
        assertThat(MDC.get(MDC_CLIENT_IP)).isNull();
    }

    @Test
    @DisplayName("요청 처리 중 예외가 나도 MDC를 비운다")
    void clearsMdcWhenChainThrows() {
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("처리 실패");
        };

        assertThatThrownBy(() -> filter.doFilter(request(), new MockHttpServletResponse(), chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(MDC_REQUEST_ID)).isNull();
        assertThat(MDC.get(MDC_CLIENT_IP)).isNull();
    }
}
