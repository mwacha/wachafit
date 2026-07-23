package com.github.mwacha.wachafit.tenant.dto;

public record TenantResponse(String id, String name, String slug, boolean active, String createdAt) {}
