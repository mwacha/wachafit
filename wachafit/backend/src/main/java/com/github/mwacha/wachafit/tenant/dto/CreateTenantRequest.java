package com.github.mwacha.wachafit.tenant.dto;

import jakarta.validation.constraints.*;

public record CreateTenantRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]*[a-z0-9]$", message = "Slug inválido") String slug
) {}
