package com.sprint.monew.user.application;

import com.sprint.monew.user.api.UserQueryApi;
import com.sprint.monew.user.api.UserSummary;
import com.sprint.monew.user.domain.EmailDuplicatedException;
import com.sprint.monew.user.domain.LoginFailedException;
import com.sprint.monew.user.domain.User;
import com.sprint.monew.user.domain.UserNotFoundException;
import com.sprint.monew.user.domain.UserRepository;
import com.sprint.monew.user.domain.UserUpdateForbiddenException;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import com.sprint.monew.user.web.dto.UserUpdateRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService implements UserQueryApi {
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

    @Transactional
    public UserDto updateNickname(UUID userId, UUID requesterId, UserUpdateRequest request) {
        if (!userId.equals(requesterId)) {
            throw new UserUpdateForbiddenException(userId);
        }
        User user = userRepository
                .findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.updateNickname(request.nickname());
        return UserDto.from(user);
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException violationException
                && User.EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(violationException.getConstraintName());
    }

    @Transactional(readOnly = true)
    @Override
    public UserSummary getSummary(UUID userId) {
        User target = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        return new UserSummary(target.getId(), target.getNickname());
    }

    @Transactional(readOnly = true)
    @Override
    public Map<UUID, UserSummary> getSummaries(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        List<User> users = userRepository.findAll().stream()
                .filter(user -> userIds.contains(user.getId()))
                .toList();

        return users.stream()
                .collect(Collectors.toMap(User::getId, user -> new UserSummary(user.getId(), user.getNickname())));
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(UUID userId) {
        return userRepository.findById(userId).map(user -> !user.isDeleted()).orElse(false);
    }
}
