package com.github.mwacha.wachafit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByStudentIdOrderByBookedAtDesc(UUID studentId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.studentId = :studentId
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.PENDING,
                           com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED)
          AND b.schedule.startsAt < :endsAt
          AND b.schedule.endsAt > :startsAt
    """)
    long countStudentOverlaps(@Param("studentId") UUID studentId,
                              @Param("startsAt") OffsetDateTime startsAt,
                              @Param("endsAt") OffsetDateTime endsAt);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.schedule.id = :scheduleId
          AND b.status = com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED
    """)
    long countConfirmedBookings(@Param("scheduleId") UUID scheduleId);
}
