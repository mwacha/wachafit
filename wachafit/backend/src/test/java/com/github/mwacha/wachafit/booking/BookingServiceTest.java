package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleStatus;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
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
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ScheduleRepository scheduleRepository;
    private BookingService service;

    @BeforeEach void setUp() {
        service = new BookingService(bookingRepository, scheduleRepository, 4L);
    }

    private Schedule buildSchedule(UUID id, ScheduleType type, ScheduleStatus status, int capacity) {
        Schedule s = new Schedule();
        try { var f = Schedule.class.getDeclaredField("id"); f.setAccessible(true); f.set(s, id); }
        catch (Exception e) { throw new RuntimeException(e); }
        s.setType(type);
        s.setStatus(status);
        s.setTrainerId(UUID.randomUUID());
        s.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(10));
        s.setEndsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(11));
        if (type == ScheduleType.CLASS) {
            GroupClass gc = new GroupClass();
            gc.setCapacity(capacity);
            s.setGroupClass(gc);
        }
        return s;
    }

    @Test
    void create_CLASS_shouldReturnConfirmed_whenCapacityAvailable() {
        UUID scheduleId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Schedule s = buildSchedule(scheduleId, ScheduleType.CLASS, ScheduleStatus.OPEN, 10);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(s));
        when(scheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(s));
        when(bookingRepository.countStudentOverlaps(eq(studentId), any(), any())).thenReturn(0L);
        when(bookingRepository.countConfirmedBookings(scheduleId)).thenReturn(5L);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            try { var f = Booking.class.getDeclaredField("id"); f.setAccessible(true); f.set(b, UUID.randomUUID()); }
            catch (Exception e) { throw new RuntimeException(e); }
            return b;
        });

        BookingResponse res = service.createBooking(new CreateBookingRequest(scheduleId), studentId);

        assertThat(res.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void create_PERSONAL_shouldReturnPending() {
        UUID scheduleId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Schedule s = buildSchedule(scheduleId, ScheduleType.PERSONAL, ScheduleStatus.OPEN, 1);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(s));
        when(scheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(s));
        when(bookingRepository.countStudentOverlaps(eq(studentId), any(), any())).thenReturn(0L);
        // PERSONAL type skips capacity check — lenient to avoid UnnecessaryStubbing
        lenient().when(bookingRepository.countConfirmedBookings(scheduleId)).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            try { var f = Booking.class.getDeclaredField("id"); f.setAccessible(true); f.set(b, UUID.randomUUID()); }
            catch (Exception e) { throw new RuntimeException(e); }
            return b;
        });

        BookingResponse res = service.createBooking(new CreateBookingRequest(scheduleId), studentId);

        assertThat(res.status()).isEqualTo("PENDING");
    }

    @Test
    void create_shouldThrow_RN01_whenStudentHasOverlap() {
        UUID scheduleId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Schedule s = buildSchedule(scheduleId, ScheduleType.CLASS, ScheduleStatus.OPEN, 10);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(s));
        when(bookingRepository.countStudentOverlaps(eq(studentId), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createBooking(new CreateBookingRequest(scheduleId), studentId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("conflito");
    }

    @Test
    void create_shouldThrow_RN03_whenClassFull() {
        UUID scheduleId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Schedule s = buildSchedule(scheduleId, ScheduleType.CLASS, ScheduleStatus.OPEN, 5);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(s));
        when(scheduleRepository.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(s));
        when(bookingRepository.countStudentOverlaps(eq(studentId), any(), any())).thenReturn(0L);
        when(bookingRepository.countConfirmedBookings(scheduleId)).thenReturn(5L);

        assertThatThrownBy(() -> service.createBooking(new CreateBookingRequest(scheduleId), studentId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("lotada");
    }

    @Test
    void cancel_shouldThrow_RN04_whenOutsideWindow() {
        UUID bookingId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Schedule s = buildSchedule(UUID.randomUUID(), ScheduleType.CLASS, ScheduleStatus.OPEN, 10);
        // Schedule starts in 2 hours — inside the 4h cancellation window
        s.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2));
        Booking b = new Booking();
        try { var f = Booking.class.getDeclaredField("id"); f.setAccessible(true); f.set(b, bookingId); }
        catch (Exception e) { throw new RuntimeException(e); }
        b.setSchedule(s);
        b.setStudentId(studentId);
        b.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancelBooking(bookingId, studentId))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("antecedência");
    }
}
