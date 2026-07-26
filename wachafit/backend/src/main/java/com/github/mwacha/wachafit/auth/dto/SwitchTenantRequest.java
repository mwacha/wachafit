package com.github.mwacha.wachafit.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchTenantRequest(
    @NotBlank String tenantId
) {}
