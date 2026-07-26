package com.github.mwacha.wachafit.auth.dto;

import java.util.List;

public record LoginResponse(
    String token,
    String role,
    String userId,
    String tenantId,
    String selectTenantToken,
    List<TenantMembershipSummary> memberships
) {
    // Construtor de 4 argumentos preservado: SignupService.java (fora de escopo) constrói
    // LoginResponse assim e não deve precisar de nenhuma alteração.
    public LoginResponse(String token, String role, String userId, String tenantId) {
        this(token, role, userId, tenantId, null, null);
    }
}
