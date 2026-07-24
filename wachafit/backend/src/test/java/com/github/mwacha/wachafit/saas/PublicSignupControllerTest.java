package com.github.mwacha.wachafit.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.saas.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest usa H2 com flyway desabilitado (application-test.yml) — os planos
// seed das migrations V32/V33 não existem nesse contexto, então o teste cria os seus.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicSignupControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired SaasPlanRepository saasPlanRepository;

    private UUID planId;

    @BeforeEach
    void setup() {
        SaasPlan plan = new SaasPlan();
        plan.setName("Pro Teste");
        plan.setDescription("Plano usado nos testes de integração");
        plan.setPrice(new BigDecimal("299.90"));
        plan.setBillingPeriodMonths(1);
        plan.setActive(true);
        planId = saasPlanRepository.save(plan).getId();
    }

    @Test
    void listPlans_isPublic() throws Exception {
        mockMvc.perform(get("/api/public/saas-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void checkSlug_returnsAvailability() throws Exception {
        mockMvc.perform(get("/api/public/check-slug").param("slug", "academia-nova-" + UUID.randomUUID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void signup_returnsCreatedWithToken() throws Exception {
        var req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "33444555000181", "11999998888",
                "academia-" + UUID.randomUUID()),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tenantId").isNotEmpty());
    }

    @Test
    void signup_conflictWhenSlugDuplicated() throws Exception {
        String slug = "academia-duplicada-" + UUID.randomUUID();
        var req = new SignupRequest(
            new SignupRequest.AdminData("Maria Admin", "maria" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Academia Fitness Ltda", "11222333000181", "11999998888", slug),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        var reqSameSlug = new SignupRequest(
            new SignupRequest.AdminData("Outro Admin", "outro" + UUID.randomUUID() + "@academia.com", "senha1234"),
            new SignupRequest.CompanyData("Outra Academia Ltda", "44555666000181", "11999998888", slug),
            new SignupRequest.PlanData(planId, PaymentMethod.PIX)
        );
        mockMvc.perform(post("/api/public/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reqSameSlug)))
            .andExpect(status().isConflict());
    }
}
