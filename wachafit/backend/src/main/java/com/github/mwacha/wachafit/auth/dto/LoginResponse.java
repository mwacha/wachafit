package com.github.mwacha.wachafit.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// NON_NULL: o frontend distingue as duas formas de resposta (token completo vs
// selectTenantToken) pela PRESENÇA das chaves no JSON, não só pelo valor -- sem isso, os campos
// nulos ainda apareceriam como "campo": null e a checagem do frontend nunca discriminaria certo.
@JsonInclude(JsonInclude.Include.NON_NULL)
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
