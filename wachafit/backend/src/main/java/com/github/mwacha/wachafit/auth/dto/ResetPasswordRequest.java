package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank(message = "Senha é obrigatória") @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String newPassword
) {}
