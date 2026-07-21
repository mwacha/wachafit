package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.booking.Booking;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock BookingRepository bookingRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @InjectMocks ReminderScheduler scheduler;

    @Test
    void sendReminders_shouldSendEmail_forBookingsIn4hWindow() {
        UUID studentId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();

        GroupClass gc = new GroupClass();
        gc.setName("Yoga");

        Schedule schedule = new Schedule();
        schedule.setType(ScheduleType.CLASS);
        schedule.setStartsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(4));
        schedule.setEndsAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(5));
        schedule.setTrainerId(trainerId);
        schedule.setGroupClass(gc);

        Booking booking = new Booking();
        booking.setStudentId(studentId);
        booking.setSchedule(schedule);

        User student = new User();
        student.setName("Maria");
        student.setEmail("maria@test.com");

        User trainer = new User();
        trainer.setName("João Personal");

        when(bookingRepository.findConfirmedBetween(any(), any())).thenReturn(List.of(booking));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(bookingRepository.save(any())).thenReturn(booking);

        scheduler.sendReminders();

        verify(emailService).sendHtml(
            eq("maria@test.com"),
            contains("Lembrete"),
            eq("email/session-reminder"),
            anyMap()
        );
        verify(bookingRepository).save(booking);
    }

    @Test
    void sendReminders_shouldNotSendEmail_whenNoBookingsInWindow() {
        when(bookingRepository.findConfirmedBetween(any(), any())).thenReturn(List.of());

        scheduler.sendReminders();

        verifyNoInteractions(emailService);
    }
}
