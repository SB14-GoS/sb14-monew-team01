package com.sprint.monew.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank String email, @NotBlank String password) {}
