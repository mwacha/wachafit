package com.github.mwacha.wachafit.report;

import com.github.mwacha.wachafit.report.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepo;

    public ReportService(ReportRepository reportRepo) {
        this.reportRepo = reportRepo;
    }

    public List<RevenueReport> getRevenue(YearMonth from, YearMonth to) {
        LocalDate fromDate = from.atDay(1);
        LocalDate toDate = to.atEndOfMonth();
        return reportRepo.getRevenueRows(fromDate, toDate).stream().map(row -> {
            LocalDate month = ((Date) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            int count = ((Number) row[2]).intValue();
            return new RevenueReport(YearMonth.of(month.getYear(), month.getMonth()), total, count);
        }).toList();
    }

    public List<OverdueStudent> getOverdueStudents() {
        return reportRepo.getOverdueStudentRows().stream().map(row -> {
            UUID studentId = UUID.fromString((String) row[0]);
            String name = (String) row[1];
            BigDecimal totalDue = (BigDecimal) row[2];
            LocalDate oldestDue = ((Date) row[3]).toLocalDate();
            int daysOverdue = (int) (LocalDate.now().toEpochDay() - oldestDue.toEpochDay());
            return new OverdueStudent(studentId, name, totalDue, daysOverdue);
        }).toList();
    }

    public SubscriptionStats getSubscriptionStats() {
        int active = 0, suspended = 0, cancelled = 0;
        for (Object[] row : reportRepo.getSubscriptionStatusCounts()) {
            String status = (String) row[0];
            int count = ((Number) row[1]).intValue();
            switch (status) {
                case "ACTIVE"    -> active    = count;
                case "SUSPENDED" -> suspended = count;
                case "CANCELLED" -> cancelled = count;
            }
        }
        int expired = reportRepo.countExpiredActive();
        return new SubscriptionStats(Math.max(0, active - expired), suspended, cancelled, expired);
    }

    public List<TrainerCommission> getTrainerCommissions(LocalDate from, LocalDate to) {
        return reportRepo.getTrainerCommissionRows(from, to).stream().map(row -> {
            UUID trainerId = UUID.fromString((String) row[0]);
            String name = (String) row[1];
            String commissionType = row[2] != null ? (String) row[2] : "NONE";
            BigDecimal commissionValue = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            int classesCount = ((Number) row[4]).intValue();
            BigDecimal commissionDue = "FIXED".equals(commissionType)
                ? commissionValue.multiply(BigDecimal.valueOf(classesCount))
                : BigDecimal.ZERO;
            return new TrainerCommission(trainerId, name, commissionType, commissionDue, classesCount);
        }).toList();
    }

    public List<EnrollmentTrend> getEnrollmentTrend(int months) {
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(months - 1);
        return reportRepo.getEnrollmentTrendRows(from).stream().map(row -> {
            String month = (String) row[0];
            int count = ((Number) row[1]).intValue();
            return new EnrollmentTrend(month, count);
        }).toList();
    }

    public List<AttendanceRank> getAttendanceRanking(int days, int limit) {
        LocalDate from = LocalDate.now().minusDays(days);
        return reportRepo.getAttendanceRankingRows(from, limit).stream().map(row -> {
            String name = (String) row[0];
            int count = ((Number) row[1]).intValue();
            return new AttendanceRank(name, count);
        }).toList();
    }

    public List<CashFlowDay> getCashFlow(LocalDate from, LocalDate to) {
        return reportRepo.getCashFlowRows(from, to).stream().map(row -> {
            LocalDate date = ((Date) row[0]).toLocalDate();
            BigDecimal received = (BigDecimal) row[1];
            BigDecimal pending  = (BigDecimal) row[2];
            BigDecimal overdue  = (BigDecimal) row[3];
            return new CashFlowDay(date, received, pending, overdue);
        }).toList();
    }
}
