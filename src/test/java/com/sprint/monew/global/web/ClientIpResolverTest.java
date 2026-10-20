package com.sprint.monew.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    @DisplayName("X-Forwarded-For에 여러 IP가 있으면 첫 번째 IP를 공백 없이 반환한다")
    void forwardedForFirstValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 203.0.113.1 , 10.0.0.1, 10.0.0.2");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For와 X-Real-IP가 모두 있으면 X-Forwarded-For를 우선한다")
    void forwardedForFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        request.addHeader("X-Real-IP", "198.51.100.7");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For가 비어 있으면 X-Real-IP를 반환한다")
    void realIpWhenForwardedForBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  ");
        request.addHeader("X-Real-IP", " 198.51.100.7 ");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("프록시 헤더가 없으면 remoteAddr를 반환한다")
    void remoteAddrWhenNoHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("192.0.2.10");
    }
}
