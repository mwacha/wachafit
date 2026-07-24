package com.github.mwacha.wachafit.saas.dto;

import com.github.mwacha.wachafit.saas.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record SignupRequest(
    @Valid @NotNull AdminData admin,
    @Valid @NotNull CompanyData company,
    @Valid @NotNull PlanData plan
) {
    public record AdminData(
        @NotBlank(message = "Nome é obrigatório") String name,
        @Email(message = "E-mail inválido") @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password
    ) {}

    public record CompanyData(
        @NotBlank(message = "Razão social é obrigatória") String name,
        @NotBlank @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos numéricos") String cnpj,
        @NotBlank(message = "Telefone é obrigatório") String phone,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9\\-]*[a-z0-9]$", message = "Slug inválido") String slug
    ) {}

    public record PlanData(
        @NotNull(message = "Selecione um plano") UUID saasPlanId,
        @NotNull(message = "Selecione uma forma de pagamento") PaymentMethod paymentMethod
    ) {}
}
