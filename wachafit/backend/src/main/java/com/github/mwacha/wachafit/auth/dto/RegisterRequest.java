package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Nome é obrigatório") String name,
    @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") String email,
    @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password
) {}
