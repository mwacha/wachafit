package com.github.mwacha.wachafit.auth.dto;

public record TenantMembershipSummary(
    String tenantId,
    String tenantName,
    String tenantSlug,
    String role
) {}
