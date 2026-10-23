package com.sprint.monew.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(result.email()).isEqualTo("woody@monew.com");
        assertThat(result.nickname()).isEqualTo("우디");
    }
}
