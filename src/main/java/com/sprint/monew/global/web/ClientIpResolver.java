package com.sprint.monew.global.web;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (hasText(forwardedFor)) {
            // 프록시 거칠 때
            // 클라이언트, 프록시1, 프록시2 -> 처음값 반환
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-Ip");
        if (hasText(realIp)) {
            // 프록시가 전달한 Ip
            return realIp.trim();
        }
        // 서버에 직접 연결된 Ip
        return request.getRemoteAddr();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
