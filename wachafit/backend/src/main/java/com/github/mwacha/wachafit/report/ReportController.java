package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.report.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public List<RevenueReport> getRevenue(
            @RequestParam String from,
            @RequestParam String to) {
        return service.getRevenue(YearMonth.parse(from), YearMonth.parse(to));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public List<OverdueStudent> getOverdue() {
        return service.getOverdueStudents();
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public SubscriptionStats getSubscriptions() {
        return service.getSubscriptionStats();
    }

    @GetMapping("/trainer-commissions")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<TrainerCommission> getTrainerCommissions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getTrainerCommissions(from, to);
    }

    @GetMapping("/enrollment-trend")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<EnrollmentTrend> getEnrollmentTrend(
            @RequestParam(defaultValue = "12") int months) {
        return service.getEnrollmentTrend(months);
    }

    @GetMapping("/attendance-ranking")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<AttendanceRank> getAttendanceRanking(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return service.getAttendanceRanking(days, limit);
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('CASHIER','MANAGER','ADMIN')")
    public List<CashFlowDay> getCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getCashFlow(from, to);
    }
}
