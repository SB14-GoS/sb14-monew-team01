package com.sprint.monew.user.application;

import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final String DUMMY_PASSWORD = "monew-timing-safe-dummy";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyEncodedPassword;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyEncodedPassword = passwordEncoder.encode(DUMMY_PASSWORD);
    }

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
        Optional<User> found = userRepository.findByEmail(request.email());
        String encodedPassword = found.map(User::getPassword).orElse(dummyEncodedPassword);
        boolean passwordMatches = passwordEncoder.matches(request.password(), encodedPassword);

        return found.filter(user -> passwordMatches && !user.isDeleted())
                .map(UserDto::from)
                .orElseThrow(LoginFailedException::new);
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violationException
                && User.EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(violationException.getConstraintName());
    }
}
