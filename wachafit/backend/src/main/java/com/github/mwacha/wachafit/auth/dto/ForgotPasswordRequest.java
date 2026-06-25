package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record ForgotPasswordRequest(
    @Email @NotBlank String email
) {}
