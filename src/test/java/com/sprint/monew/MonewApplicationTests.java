package com.sprint.monew;

import com.sprint.monew.article.api.ArticleCommandApi;
import com.sprint.monew.article.api.ArticleQueryApi;
import com.sprint.monew.support.IntegrationTest;
import com.sprint.monew.user.api.UserQueryApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
class MonewApplicationTests {

    @MockitoBean
    ArticleQueryApi articleQueryApi;

    @MockitoBean
    ArticleCommandApi articleCommandApi;

    @MockitoBean
    UserQueryApi userQueryApi;

    @Test
    void contextLoads() {}
}
