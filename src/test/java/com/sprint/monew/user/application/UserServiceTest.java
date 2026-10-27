package com.sprint.monew.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserDeleteForbiddenException;
import com.sprint.monew.user.domain.UserNotFoundException;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.domain.UserUpdateForbiddenException;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import com.sprint.monew.user.web.dto.UserUpdateRequest;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다.")
    void registerWithDuplicatedEmail() {
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");

        given(userRepository.existsByEmail("woody@monew.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request)).isInstanceOf(EmailDuplicatedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("회원가입하면 비밀번호를 암호화해 저장한다.")
    void register() {
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");
        given(userRepository.existsByEmail("woody@monew.com")).willReturn(false);
        given(passwordEncoder.encode("monew1!")).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(result.email()).isEqualTo("woody@monew.com");
        assertThat(result.nickname()).isEqualTo("우디");
    }

    @Test
    @DisplayName("동시 가입으로 이메일 unique 제약에 걸리면 이메일 중복 예외로 바꾼다")
    void registerWithConcurrentDuplicatedEmail() {
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");
        given(userRepository.existsByEmail("woody@monew.com")).willReturn(false);
        given(passwordEncoder.encode("monew1!")).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException(
                        "duplicate",
                        new ConstraintViolationException(
                                "duplicate", new SQLException(), User.EMAIL_UNIQUE_CONSTRAINT)));

        assertThatThrownBy(() -> userService.register(request)).isInstanceOf(EmailDuplicatedException.class);
    }

    @Test
    @DisplayName("이메일이 아닌 다른 무결성 오류는 그대로 전파한다")
    void registerWithOtherIntegrityViolation() {
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");
        given(userRepository.existsByEmail("woody@monew.com")).willReturn(false);
        given(passwordEncoder.encode("monew1!")).willReturn("encoded-password");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException(
                        "other", new ConstraintViolationException("other", new SQLException(), "other_constraint")));

        assertThatThrownBy(() -> userService.register(request)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("이메일과 비밀번호가 맞으면 로그인에 성공한다")
    void login() {
        User user = new User("woody@monew.com", "우디", "encoded-password");
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("monew1!", "encoded-password")).willReturn(true);

        UserDto result = userService.login(new UserLoginRequest("woody@monew.com", "monew1!"));

        assertThat(result.email()).isEqualTo("woody@monew.com");
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 로그인에 실패한다")
    void loginWithUnknownEmail() {
        given(userRepository.findByEmail("none@monew.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("none@monew.com", "monew1!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인에 실패한다")
    void loginWithWrongPassword() {
        User user = new User("woody@monew.com", "우디", "encoded-password");
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong!!", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("woody@monew.com", "wrong!!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 로그인할 수 없다")
    void loginWithDeletedUser() {
        User user = new User("woody@monew.com", "우디", "encoded-password");
        user.softDelete();
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("woody@monew.com", "monew1!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("가입되지 않은 이메일이어도 비밀번호 검증을 수행한다 (응답 시간 차이 방지)")
    void loginWithUnknownEmailStillChecksPassword() {
        given(userRepository.findByEmail("none@monew.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("none@monew.com", "monew1!")))
                .isInstanceOf(LoginFailedException.class);
        verify(passwordEncoder).matches(eq("monew1!"), any());
    }

    @Test
    @DisplayName("탈퇴한 사용자여도 비밀번호 검증을 수행한다 (응답 시간 차이 방지)")
    void loginWithDeletedUserStillChecksPassword() {
        User user = new User("woody@monew.com", "우디", "encoded-password");
        user.softDelete();
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("woody@monew.com", "monew1!")))
                .isInstanceOf(LoginFailedException.class);
        verify(passwordEncoder).matches("monew1!", "encoded-password");
    }

    @Test
    @DisplayName("본인이면 닉네임을 수정한다")
    void updateNickname() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "encoded-password");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        UserDto result = userService.updateNickname(userId, userId, new UserUpdateRequest("보안관우디"));

        assertThat(result.nickname()).isEqualTo("보안관우디");
        assertThat(user.getNickname()).isEqualTo("보안관우디");
    }

    @Test
    @DisplayName("다른 사용자의 정보는 수정할 수 없다")
    void updateNicknameOfOtherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.updateNickname(userId, otherUserId, new UserUpdateRequest("버즈")))
                .isInstanceOf(UserUpdateForbiddenException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 수정할 수 없다")
    void updateNicknameOfUnknownUser() {
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateNickname(userId, userId, new UserUpdateRequest("우디")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 수정할 수 없다")
    void updateNicknameOfDeletedUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "encoded-password");
        user.softDelete();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateNickname(userId, userId, new UserUpdateRequest("우디")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("본인이면 논리 삭제하고 삭제 시각을 기록한다")
    void softDelete() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "encoded-password");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        userService.softDelete(userId, userId);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 사용자는 삭제할 수 없다")
    void softDeleteOtherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.softDelete(userId, otherUserId))
                .isInstanceOf(UserDeleteForbiddenException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 삭제할 수 없다")
    void softDeleteUnknownUser() {
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.softDelete(userId, userId)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자는 다시 삭제할 수 없다")
    void softDeleteAlreadyDeletedUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "encoded-password");
        user.softDelete();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.softDelete(userId, userId)).isInstanceOf(UserNotFoundException.class);
    }
}
