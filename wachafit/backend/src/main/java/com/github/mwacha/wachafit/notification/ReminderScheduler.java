package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.booking.Booking;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
public class ReminderScheduler {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ReminderScheduler(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void sendReminders() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime from = now.plusHours(3);
        OffsetDateTime to   = now.plusHours(5);
        List<Booking> bookings = bookingRepository.findConfirmedBetween(from, to);
        for (Booking booking : bookings) {
            userRepository.findById(booking.getStudentId()).ifPresent(student ->
                userRepository.findById(booking.getSchedule().getTrainerId()).ifPresent(trainer ->
                    emailService.sendHtml(
                        student.getEmail(),
                        "Lembrete de sessão — WachaFit",
                        "email/session-reminder",
                        Map.of(
                            "name",        student.getName(),
                            "className",   booking.getSchedule().getGroupClass() != null
                                               ? booking.getSchedule().getGroupClass().getName()
                                               : "Treino Personal",
                            "date",        booking.getSchedule().getStartsAt()
                                               .toLocalDate().toString(),
                            "time",        booking.getSchedule().getStartsAt()
                                               .toLocalTime().toString(),
                            "trainerName", trainer.getName()
                        )
                    )
                )
            );
        }
    }
}
