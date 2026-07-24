package com.github.mwacha.wachafit.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.saas.dto.CreateSaasPlanRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaasPlanControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createPlan_returnsCreated() throws Exception {
        var req = new CreateSaasPlanRequest("Starter", "Plano inicial", new BigDecimal("149.90"), 1, 5);
        mockMvc.perform(post("/api/super/saas-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Starter"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPlan_forbiddenForAdmin() throws Exception {
        var req = new CreateSaasPlanRequest("Starter", "Plano inicial", new BigDecimal("149.90"), 1, 5);
        mockMvc.perform(post("/api/super/saas-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listPlans_returnsList() throws Exception {
        mockMvc.perform(get("/api/super/saas-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
