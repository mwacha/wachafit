package com.github.mwacha.wachafit.user.dto;

public record UserResponse(
    String id,
    String name,
    String email,
    String role,
    boolean active,
    String createdAt
) {}
