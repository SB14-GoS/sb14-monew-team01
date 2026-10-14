package com.sprint.monew.global.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaForwardController.class)
class SpaForwardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest(name = "{0} 요청은 index.html로 전달된다")
    @ValueSource(strings = {"/", "/login", "/signup", "/articles", "/articles/abc", "/interests", "/activities"})
    void forwardsToIndex(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }
}
