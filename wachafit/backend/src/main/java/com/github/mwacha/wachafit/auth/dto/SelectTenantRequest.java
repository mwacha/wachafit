package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectTenantRequest(
    @NotBlank String selectTenantToken,
    @NotBlank String tenantId
) {}
