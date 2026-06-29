package com.github.mwacha.wachafit.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mwacha.wachafit.auth.dto.LoginRequest;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers @ActiveProfiles("test")
class ProgressControllerIntegrationTest {

    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("jwt.secret", () -> "integration-test-secret-32-chars-ok");
        r.add("jwt.expiration", () -> "3600");
        r.add("app.frontend-url", () -> "http://localhost:5173");
        r.add("app.upload-dir", () -> System.getProperty("java.io.tmpdir") + "/wachafit-test-uploads");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private String studentToken;
    private UUID studentId;

    @BeforeEach
    void setUp() throws Exception {
        userRepo.deleteAll();
        User trainer = new User(); trainer.setName("T"); trainer.setEmail("t@t.com");
        trainer.setPasswordHash(passwordEncoder.encode("pass")); trainer.setRole(Role.TRAINER); trainer.setActive(true);
        userRepo.save(trainer);
        User student = new User(); student.setName("S"); student.setEmail("s@t.com");
        student.setPasswordHash(passwordEncoder.encode("pass")); student.setRole(Role.STUDENT); student.setActive(true);
        userRepo.save(student);
        studentId = student.getId();
        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new LoginRequest("s@t.com", "pass")))).andReturn();
        studentToken = mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void upload_withStudentToken_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
            MediaType.IMAGE_JPEG_VALUE, "fake-image-bytes".getBytes());
        mvc.perform(multipart("/api/students/" + studentId + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.studentId").value(studentId.toString()))
            .andExpect(jsonPath("$.fileUrl").isString());
    }

    @Test
    void list_withStudentToken_shouldReturn200() throws Exception {
        mvc.perform(get("/api/students/" + studentId + "/photos")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void upload_withoutToken_shouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
            MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes());
        mvc.perform(multipart("/api/students/" + UUID.randomUUID() + "/photos").file(file))
            .andExpect(status().isUnauthorized());
    }
}
