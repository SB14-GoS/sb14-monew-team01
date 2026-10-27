package com.sprint.monew.user.web;

import com.sprint.monew.global.web.RequestUserId;
import com.sprint.monew.user.application.UserService;
import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import com.sprint.monew.user.web.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApiDocs {
    private final UserService userService;

    @Override
    @PostMapping("/api/users")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegisterRequest request) {
        UserDto result = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Override
    @PostMapping("/api/users/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody UserLoginRequest request) {
        UserDto result = userService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Override
    @PatchMapping("/api/users/{userId}")
    public ResponseEntity<UserDto> updateNickname(
            @PathVariable UUID userId, @RequestUserId UUID requesterId, @Valid @RequestBody UserUpdateRequest request) {
        UserDto result = userService.updateNickname(userId, requesterId, request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Override
    @DeleteMapping("/api/users/{userId}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID userId, @RequestUserId UUID requesterId) {
        userService.softDelete(userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/api/users/{userId}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable UUID userId, @RequestUserId UUID requesterId) {
        userService.hardDelete(userId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
