package com.sprint.monew.user.web;

import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserLoginRequest;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import com.sprint.monew.user.web.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 관리")
public interface UserApiDocs {
    @Operation(summary = "회원가입")
    ResponseEntity<UserDto> register(UserRegisterRequest request);

    @Operation(summary = "로그인")
    ResponseEntity<UserDto> login(UserLoginRequest request);

    @Operation(summary = "사용자 정보 수정")
    ResponseEntity<UserDto> updateNickname(UUID userId, UUID requesterId, UserUpdateRequest request);

    @Operation(summary = "사용자 논리 삭제")
    ResponseEntity<Void> softDelete(UUID userId, UUID requesterId);
}
