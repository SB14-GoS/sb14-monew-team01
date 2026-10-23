package com.sprint.monew.user.application;

import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            User user = userRepository.saveAndFlush(new User(request.email(), request.nickname(), encodedPassword));
            return UserDto.from(user);
        } catch (DataIntegrityViolationException e) {
            if (isEmailUniqueViolation(e)) {
                throw new EmailDuplicatedException(request.email());
            }
            throw e;
        }
    }

    public UserDto login(UserLoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .filter(found -> !found.isDeleted())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(LoginFailedException::new);
        return UserDto.from(user);
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violationException
                && User.EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(violationException.getConstraintName());
    }
}
