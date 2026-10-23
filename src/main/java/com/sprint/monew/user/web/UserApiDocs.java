package com.sprint.monew.user.web;

import com.sprint.monew.user.web.dto.UserDto;
import com.sprint.monew.user.web.dto.UserRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 관리")
public interface UserApiDocs {
    @Operation(summary = "회원가입")
    ResponseEntity<UserDto> register(UserRegisterRequest request);
}
