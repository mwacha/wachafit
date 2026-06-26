package com.github.mwacha.wachafit.schedule;

import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.schedule.dto.ScheduleRequest;
import com.github.mwacha.wachafit.schedule.dto.ScheduleResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock TrainerAvailabilityRepository availabilityRepository;
    @Mock GroupClassRepository groupClassRepository;
    private ScheduleService service;

    @BeforeEach void setUp() {
        service = new ScheduleService(scheduleRepository, availabilityRepository, groupClassRepository);
    }

    @Test
    void create_shouldSucceed_whenNoOverlap() {
        UUID trainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        GroupClass gc = new GroupClass();
        gc.setCapacity(10);
        // Set the GroupClass id via reflection so toResponse doesn't NPE
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        when(groupClassRepository.findById(classId)).thenReturn(Optional.of(gc));
        when(scheduleRepository.countOverlaps(trainerId, start, end)).thenReturn(0L);
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            try { var f = Schedule.class.getDeclaredField("id"); f.setAccessible(true); f.set(s, UUID.randomUUID()); }
            catch (Exception e) { throw new RuntimeException(e); }
            return s;
        });

        ScheduleResponse res = service.create(
            new ScheduleRequest(classId, trainerId, ScheduleType.CLASS, start, end));

        assertThat(res).isNotNull();
    }

    @Test
    void create_shouldThrow_whenTrainerHasOverlap() {
        UUID trainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        // No need to stub groupClassRepository — exception is thrown before buildSchedule is called
        when(scheduleRepository.countOverlaps(trainerId, start, end)).thenReturn(1L);

        assertThatThrownBy(() -> service.create(
            new ScheduleRequest(classId, trainerId, ScheduleType.CLASS, start, end)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("conflito");
    }
}
