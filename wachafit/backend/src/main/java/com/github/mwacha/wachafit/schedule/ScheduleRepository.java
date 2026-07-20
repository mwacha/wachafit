package com.github.mwacha.wachafit.schedule;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("""
        SELECT COUNT(s) FROM Schedule s
        WHERE s.trainerId = :trainerId
          AND s.status != 'CANCELLED'
          AND s.startsAt < :endsAt
          AND s.endsAt > :startsAt
    """)
    long countOverlaps(@Param("trainerId") UUID trainerId,
                       @Param("startsAt") OffsetDateTime startsAt,
                       @Param("endsAt") OffsetDateTime endsAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT s FROM Schedule s
        WHERE s.groupClass.id = :classId
          AND s.startsAt >= :now
          AND s.status <> com.github.mwacha.wachafit.schedule.ScheduleStatus.CANCELLED
        ORDER BY s.startsAt
    """)
    List<Schedule> findUpcomingByClassId(@Param("classId") UUID classId,
                                          @Param("now") OffsetDateTime now);

    @Query(value = """
        SELECT * FROM schedules s
        WHERE (CAST(:from AS timestamptz) IS NULL OR s.starts_at >= CAST(:from AS timestamptz))
          AND (CAST(:to AS timestamptz) IS NULL OR s.ends_at <= CAST(:to AS timestamptz))
          AND (CAST(:date AS date) IS NULL OR CAST(s.starts_at AS date) = CAST(:date AS date))
          AND (CAST(:trainerId AS uuid) IS NULL OR s.trainer_id = CAST(:trainerId AS uuid))
          AND (CAST(:type AS varchar) IS NULL OR s.type = CAST(:type AS varchar))
          AND s.status != 'CANCELLED'
        ORDER BY s.starts_at
    """, nativeQuery = true)
    List<Schedule> findByFilters(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        @Param("date") LocalDate date,
        @Param("trainerId") UUID trainerId,
        @Param("type") String type
    );
}
