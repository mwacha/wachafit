package com.github.mwacha.wachafit.auth.dto;

public record LoginResponse(String token, String role, String userId) {}
