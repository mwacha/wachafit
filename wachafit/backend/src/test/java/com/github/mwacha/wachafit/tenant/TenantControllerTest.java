package com.github.mwacha.wachafit.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.tenant.dto.CreateTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createTenant_returnsCreated() throws Exception {
        var req = new CreateTenantRequest("Academia Fitness", "academia-fitness");
        mockMvc.perform(post("/api/super/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("academia-fitness"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTenant_forbiddenForAdmin() throws Exception {
        var req = new CreateTenantRequest("Outra", "outra");
        mockMvc.perform(post("/api/super/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void listTenants_returnsList() throws Exception {
        mockMvc.perform(get("/api/super/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
