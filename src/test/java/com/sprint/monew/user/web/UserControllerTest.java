package com.sprint.monew.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.user.application.UserService;
import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.domain.UserUpdateForbiddenException;
import com.sprint.monew.user.web.dto.UserDto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @Test
    @DisplayName("닉네임 수정에 성공하면 200과 수정된 사용자 정보를 반환한다")
    void updateNickname() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "woody@monew.com", "보안관우디", Instant.now());
        given(userService.updateNickname(eq(userId), eq(userId), any())).willReturn(userDto);

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "보안관우디"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("보안관우디"));
    }

    @Test
    @DisplayName("다른 사용자의 정보를 수정하면 403을 반환한다")
    void updateNicknameForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        given(userService.updateNickname(eq(userId), eq(otherUserId), any()))
                .willThrow(new UserUpdateForbiddenException(userId));

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", otherUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "버즈"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_UPDATE_FORBIDDEN"));
    }

    @ParameterizedTest(name = "{0} → 400")
    @ValueSource(strings = {"{\"nickname\": \"\"}", "{\"nickname\": \"   \"}", "{\"nickname\": null}", "{}"})
    @DisplayName("닉네임이 비었거나 공백·null·누락이면 400을 반환한다")
    void updateNicknameWithInvalidNickname(String body) throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verify(userService, never()).updateNickname(any(), any(), any());
    }

    @Test
    @DisplayName("닉네임이 20자면 수정에 성공한다")
    void updateNicknameWithMaxLength() throws Exception {
        UUID userId = UUID.randomUUID();
        String nickname = "가".repeat(20);
        UserDto userDto = new UserDto(userId, "woody@monew.com", nickname, Instant.now());
        given(userService.updateNickname(eq(userId), eq(userId), any())).willReturn(userDto);

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "%s"}
                                """.formatted(nickname)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("닉네임이 21자면 400을 반환한다")
    void updateNicknameWithTooLongNickname() throws Exception {
        UUID userId = UUID.randomUUID();
        String nickname = "가".repeat(21);

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("Monew-Request-User-ID", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "%s"}
                                """.formatted(nickname)))
                .andExpect(status().isBadRequest());
    }
}
