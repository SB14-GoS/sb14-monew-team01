package com.sprint.monew.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.user.application.UserService;
import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.web.dto.UserDto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    @DisplayName("회원가입에 성공하면 201과 사용자 정보를 반환한다.")
    void register() throws Exception {
        UserDto userDto = new UserDto(UUID.randomUUID(), "woody@monew.com", "우디", Instant.now());
        given(userService.register(any())).willReturn(userDto);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "woody@monew.com", "nickname": "우디", "password": "monew1!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("woody@monew.com"))
                .andExpect(jsonPath("$.nickname").value("우디"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("입력값이 올바르지 않으면 400을 반환한다")
    void registerWithInvalidInput() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-email", "nickname": "", "password": "123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일이 중복되면 409를 반환한다")
    void registerWithDuplicatedEmail() throws Exception {
        given(userService.register(any())).willThrow(new EmailDuplicatedException("woody@monew.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "woody@monew.com", "nickname": "우디", "password": "monew1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"));
    }

    @Test
    @DisplayName("로그인에 성공하면 200과 사용자 정보를 반환한다")
    void login() throws Exception {
        UserDto userDto = new UserDto(UUID.randomUUID(), "woody@monew.com", "우디", Instant.now());
        given(userService.login(any())).willReturn(userDto);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "woody@monew.com", "password": "monew1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("woody@monew.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("로그인에 실패하면 401을 반환한다")
    void loginFailed() throws Exception {
        given(userService.login(any())).willThrow(new LoginFailedException());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "woody@monew.com", "password": "wrong!!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }
}
