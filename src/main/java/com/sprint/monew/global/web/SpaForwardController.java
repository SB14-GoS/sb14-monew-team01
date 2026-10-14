package com.sprint.monew.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 제공 프론트엔드(React SPA)의 화면 경로를 index.html로 넘긴다.
 *
 * <p>브라우저에서 /articles 같은 화면 주소로 바로 접속하거나 새로고침해도 404가 나지 않게 한다. API(/api/**)와 정적 파일은 영향받지
 * 않는다.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/signup", "/articles", "/articles/{articleId}", "/interests", "/activities"})
    public String forward() {
        return "forward:/index.html";
    }
}
