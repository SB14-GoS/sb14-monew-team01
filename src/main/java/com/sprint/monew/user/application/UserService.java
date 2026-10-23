package com.sprint.monew.user.application;

import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailDuplicatedException(request.email());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(request.email(), request.nickname(), encodedPassword));
        return UserDto.from(user);
    }
}
