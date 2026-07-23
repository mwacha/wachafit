package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String tenantSlug
) {}
