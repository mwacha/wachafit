package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.notification.EmailService;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleStatus;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public BookingResponse createBooking(CreateBookingRequest req, UUID studentId) {
        Schedule schedule = scheduleRepository.findById(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException("Este schedule foi cancelado");
        }
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            throw new BusinessException("Turma lotada");
        }

        long studentOverlap = bookingRepository.countStudentOverlaps(
            studentId, schedule.getStartsAt(), schedule.getEndsAt());
        if (studentOverlap > 0) {
            throw new BusinessException("Você já tem um agendamento neste horário (conflito)");
        }

        Schedule locked = scheduleRepository.findByIdForUpdate(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        if (locked.getType() == ScheduleType.CLASS) {
            int capacity = locked.getGroupClass().getCapacity();
            long confirmed = bookingRepository.countConfirmedBookings(locked.getId());
            if (confirmed >= capacity) {
                throw new BusinessException("Turma lotada");
            }
            if (confirmed + 1 >= capacity) {
                locked.setStatus(ScheduleStatus.FULL);
                scheduleRepository.save(locked);
            }
        }

        BookingStatus status = locked.getType() == ScheduleType.CLASS
            ? BookingStatus.CONFIRMED
            : BookingStatus.PENDING;

        Booking booking = new Booking();
        booking.setSchedule(locked);
        booking.setStudentId(studentId);
        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);

        if (status == BookingStatus.CONFIRMED) {
            sendBookingConfirmedEmail(studentId, locked);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID studentId) {
        return bookingRepository.findByStudentIdOrderByBookedAtDesc(studentId)
            .stream().map(this::toResponse).toList();
    }

    public void cancelBooking(UUID bookingId, UUID requestingUserId, Role requestingRole) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking não encontrado"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking já cancelado");
        }

        if (requestingRole == Role.STUDENT && !booking.getStudentId().equals(requestingUserId)) {
            throw new ForbiddenException("Access denied");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Schedule schedule = booking.getSchedule();
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            schedule.setStatus(ScheduleStatus.OPEN);
            scheduleRepository.save(schedule);
        }
        bookingRepository.save(booking);

        sendBookingCancelledEmail(booking.getStudentId(), schedule);
    }

    private void sendBookingConfirmedEmail(UUID studentId, Schedule schedule) {
        userRepository.findById(studentId).ifPresent(student ->
            userRepository.findById(schedule.getTrainerId()).ifPresent(trainer ->
                emailService.sendHtml(
                    student.getEmail(),
                    "Agendamento confirmado — WachaFit",
                    "email/booking-confirmed",
                    Map.of(
                        "name", student.getName(),
                        "className", schedule.getGroupClass() != null
                            ? schedule.getGroupClass().getName() : "Sessão individual",
                        "date", schedule.getStartsAt().toLocalDate().toString(),
                        "time", schedule.getStartsAt().toLocalTime().toString(),
                        "trainerName", trainer.getName()
                    )
                )
            )
        );
    }

    private void sendBookingCancelledEmail(UUID studentId, Schedule schedule) {
        userRepository.findById(studentId).ifPresent(student ->
            emailService.sendHtml(
                student.getEmail(),
                "Agendamento cancelado — WachaFit",
                "email/booking-cancelled",
                Map.of(
                    "name", student.getName(),
                    "className", schedule.getGroupClass() != null
                        ? schedule.getGroupClass().getName() : "Sessão individual",
                    "date", schedule.getStartsAt().toLocalDate().toString(),
                    "time", schedule.getStartsAt().toLocalTime().toString()
                )
            )
        );
    }

    private BookingResponse toResponse(Booking b) {
        Schedule s = b.getSchedule();
        return new BookingResponse(
            b.getId().toString(),
            s.getId().toString(),
            s.getStartsAt().toString(),
            s.getEndsAt().toString(),
            s.getType().name(),
            b.getStatus().name(),
            s.getGroupClass() != null ? s.getGroupClass().getName() : null,
            null,
            b.getBookedAt() != null ? b.getBookedAt().toString() : null
        );
    }
}
