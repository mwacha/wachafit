package com.github.mwacha.wachafit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.studentId = :studentId
          AND b.schedule.type = com.github.mwacha.wachafit.schedule.ScheduleType.PERSONAL
        ORDER BY b.bookedAt DESC
    """)
    List<Booking> findPersonalByStudentIdOrderByBookedAtDesc(@Param("studentId") UUID studentId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.studentId = :studentId
          AND b.schedule.type = com.github.mwacha.wachafit.schedule.ScheduleType.PERSONAL
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

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED
          AND b.reminderSent = false
          AND b.schedule.startsAt >= :from
          AND b.schedule.startsAt <= :to
    """)
    List<Booking> findConfirmedBetween(@Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.schedule.id = :scheduleId
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    List<Booking> findActiveByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.schedule.id = :scheduleId
          AND b.studentId = :studentId
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    long countActiveByScheduleAndStudent(@Param("scheduleId") UUID scheduleId,
                                         @Param("studentId") UUID studentId);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.schedule.id IN :scheduleIds
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    List<Booking> findActiveByScheduleIds(@Param("scheduleIds") List<UUID> scheduleIds);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.schedule.id IN :scheduleIds
          AND b.studentId = :studentId
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    List<Booking> findActiveByScheduleIdsAndStudentId(@Param("scheduleIds") List<UUID> scheduleIds,
                                                       @Param("studentId") UUID studentId);

    @Query("""
        SELECT COUNT(DISTINCT b.studentId) FROM Booking b
        WHERE b.schedule.groupClass.id = :classId
          AND b.schedule.startsAt >= :now
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    long countEnrolledStudentsByClassId(@Param("classId") UUID classId,
                                        @Param("now") java.time.OffsetDateTime now);

    @Query("""
        SELECT COUNT(DISTINCT b.schedule.groupClass.id) FROM Booking b
        WHERE b.studentId = :studentId
          AND b.schedule.startsAt >= :now
          AND b.schedule.groupClass IS NOT NULL
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    long countEnrolledClassesByStudentId(@Param("studentId") UUID studentId,
                                         @Param("now") java.time.OffsetDateTime now);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.schedule.groupClass.id = :classId
          AND b.studentId = :studentId
          AND b.schedule.startsAt >= :now
          AND b.status IN (com.github.mwacha.wachafit.booking.BookingStatus.CONFIRMED,
                           com.github.mwacha.wachafit.booking.BookingStatus.PENDING)
    """)
    long countActiveByClassIdAndStudentId(@Param("classId") UUID classId,
                                          @Param("studentId") UUID studentId,
                                          @Param("now") java.time.OffsetDateTime now);
}
