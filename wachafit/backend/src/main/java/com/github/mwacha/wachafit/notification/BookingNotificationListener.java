package com.github.mwacha.wachafit.notification;

import com.github.mwacha.wachafit.notification.event.BookingCancelledEvent;
import com.github.mwacha.wachafit.notification.event.BookingConfirmedEvent;
import com.github.mwacha.wachafit.notification.event.PersonalSessionRequestedEvent;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class BookingNotificationListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public BookingNotificationListener(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        userRepository.findById(event.studentId()).ifPresent(student ->
            emailService.sendHtml(
                student.getEmail(),
                "Agendamento confirmado — WachaFit",
                "email/booking-confirmed",
                Map.of(
                    "name",        student.getName(),
                    "className",   event.className(),
                    "date",        event.date(),
                    "time",        event.time(),
                    "trainerName", userRepository.findById(event.trainerId())
                                       .map(t -> t.getName()).orElse("Personal")
                )
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        userRepository.findById(event.studentId()).ifPresent(student ->
            emailService.sendHtml(
                student.getEmail(),
                "Agendamento cancelado — WachaFit",
                "email/booking-cancelled",
                Map.of(
                    "name",      student.getName(),
                    "className", event.className(),
                    "date",      event.date(),
                    "time",      event.time()
                )
            )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPersonalSessionRequested(PersonalSessionRequestedEvent event) {
        userRepository.findById(event.trainerId()).ifPresent(trainer ->
            userRepository.findById(event.studentId()).ifPresent(student ->
                emailService.sendHtml(
                    trainer.getEmail(),
                    "Nova solicitação de sessão — WachaFit",
                    "email/trainer-personal-request",
                    Map.of(
                        "trainerName", trainer.getName(),
                        "studentName", student.getName(),
                        "date",        event.date(),
                        "time",        event.time()
                    )
                )
            )
        );
    }
}
