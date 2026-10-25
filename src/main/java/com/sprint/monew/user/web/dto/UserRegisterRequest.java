package com.sprint.monew.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 1, max = 20) String nickname,
        @NotBlank @Size(min = 6, max = 20) String password) {}
