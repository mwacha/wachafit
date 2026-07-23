package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ReportRepository.class)
@ActiveProfiles("test")
class ReportRepositoryTest {

    @Autowired ReportRepository reportRepository;

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void getSubscriptionStatusCounts_returnEmptyWithoutData() {
        TenantContext.set(UUID.randomUUID());
        // Não deve explodir; pode retornar lista vazia
        assertThat(reportRepository.getSubscriptionStatusCounts()).isNotNull();
    }
}
