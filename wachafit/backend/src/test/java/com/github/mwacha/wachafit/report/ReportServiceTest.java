package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.report.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock ReportRepository reportRepo;
    @InjectMocks ReportService service;

    @Test
    void getRevenue_shouldMapRowsToRevenueReports() {
        Object[] row = { Date.valueOf(LocalDate.of(2026, 1, 1)), new BigDecimal("500.00"), 5L };
        when(reportRepo.getRevenueRows(any(), any())).thenReturn(List.<Object[]>of(row));

        List<RevenueReport> result = service.getRevenue(YearMonth.of(2026, 1), YearMonth.of(2026, 3));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).month()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(result.get(0).total()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).chargesCount()).isEqualTo(5);
    }

    @Test
    void getSubscriptionStats_shouldAggregateByStatusAndCountExpired() {
        when(reportRepo.getSubscriptionStatusCounts()).thenReturn(List.<Object[]>of(
            new Object[]{"ACTIVE", 10L},
            new Object[]{"SUSPENDED", 2L},
            new Object[]{"CANCELLED", 5L}
        ));
        when(reportRepo.countExpiredActive()).thenReturn(3);

        SubscriptionStats stats = service.getSubscriptionStats();

        assertThat(stats.active()).isEqualTo(7);
        assertThat(stats.suspended()).isEqualTo(2);
        assertThat(stats.cancelled()).isEqualTo(5);
        assertThat(stats.expired()).isEqualTo(3);
    }

    @Test
    void getOverdueStudents_shouldComputeDaysOverdue() {
        LocalDate dueDate = LocalDate.now().minusDays(10);
        UUID studentId = UUID.randomUUID();
        Object[] row = { studentId.toString(), "João Silva", new BigDecimal("200.00"), Date.valueOf(dueDate) };
        when(reportRepo.getOverdueStudentRows()).thenReturn(List.<Object[]>of(row));

        List<OverdueStudent> result = service.getOverdueStudents();

        assertThat(result.get(0).daysOverdue()).isEqualTo(10);
        assertThat(result.get(0).totalDue()).isEqualByComparingTo("200.00");
        assertThat(result.get(0).name()).isEqualTo("João Silva");
    }

    @Test
    void getCashFlow_shouldMapRowsToCashFlowDays() {
        Object[] row = {
            Date.valueOf(LocalDate.of(2026, 6, 1)),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            new BigDecimal("25.00")
        };
        when(reportRepo.getCashFlowRows(any(), any())).thenReturn(List.<Object[]>of(row));

        List<CashFlowDay> result = service.getCashFlow(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(result.get(0).received()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).pending()).isEqualByComparingTo("50.00");
        assertThat(result.get(0).overdue()).isEqualByComparingTo("25.00");
    }

    @Test
    void getTrainerCommissions_fixedType_shouldMultiplyByClassCount() {
        UUID trainerId = UUID.randomUUID();
        Object[] row = { trainerId.toString(), "Ana Trainer", "FIXED", new BigDecimal("50.00"), 4L };
        when(reportRepo.getTrainerCommissionRows(any(), any())).thenReturn(List.<Object[]>of(row));

        List<TrainerCommission> result = service.getTrainerCommissions(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(result.get(0).commissionDue()).isEqualByComparingTo("200.00"); // 50 × 4
        assertThat(result.get(0).classesCount()).isEqualTo(4);
        assertThat(result.get(0).commissionType()).isEqualTo("FIXED");
    }

    @Test
    void getTrainerCommissions_percentageType_shouldReturnZeroDue() {
        UUID trainerId = UUID.randomUUID();
        Object[] row = { trainerId.toString(), "Carlos Trainer", "PERCENTAGE", new BigDecimal("10.00"), 3L };
        when(reportRepo.getTrainerCommissionRows(any(), any())).thenReturn(List.<Object[]>of(row));

        List<TrainerCommission> result = service.getTrainerCommissions(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(result.get(0).commissionDue()).isEqualByComparingTo("0.00");
        assertThat(result.get(0).commissionType()).isEqualTo("PERCENTAGE");
    }
}
