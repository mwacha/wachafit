package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank(message = "Nome é obrigatório") String name,
    @Email(message = "E-mail inválido") @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String tenantSlug
) {}
