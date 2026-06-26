package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleStatus;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final long cancellationWindowHours;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          @Value("${app.cancellation-window-hours:4}") long cancellationWindowHours) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.cancellationWindowHours = cancellationWindowHours;
    }

    public BookingResponse createBooking(CreateBookingRequest req, UUID studentId) {
        // 1. Load schedule (not locked yet)
        Schedule schedule = scheduleRepository.findById(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        if (schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException("Este schedule foi cancelado");
        }
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            throw new BusinessException("Turma lotada");
        }

        // 2. RN-01: check student overlap (no lock needed, read-only check)
        long studentOverlap = bookingRepository.countStudentOverlaps(
            studentId, schedule.getStartsAt(), schedule.getEndsAt());
        if (studentOverlap > 0) {
            throw new BusinessException("Você já tem um agendamento neste horário (conflito)");
        }

        // 3. Pessimistic lock on the schedule row (RN-02/RN-03)
        Schedule locked = scheduleRepository.findByIdForUpdate(req.scheduleId())
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado"));

        // 4. RN-02: check capacity for CLASS type (after acquiring lock)
        if (locked.getType() == ScheduleType.CLASS) {
            int capacity = locked.getGroupClass().getCapacity();
            long confirmed = bookingRepository.countConfirmedBookings(locked.getId());
            if (confirmed >= capacity) {
                throw new BusinessException("Turma lotada");
            }

            // 5. RN-03: auto-FULL — mark schedule FULL if this booking fills the last slot
            if (confirmed + 1 >= capacity) {
                locked.setStatus(ScheduleStatus.FULL);
                scheduleRepository.save(locked);
            }
        }

        // 6. Determine booking status
        BookingStatus status = locked.getType() == ScheduleType.CLASS
            ? BookingStatus.CONFIRMED
            : BookingStatus.PENDING;

        // 7. Persist booking
        Booking booking = new Booking();
        booking.setSchedule(locked);
        booking.setStudentId(studentId);
        booking.setStatus(status);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID studentId) {
        return bookingRepository.findByStudentIdOrderByBookedAtDesc(studentId)
            .stream().map(this::toResponse).toList();
    }

    public void cancelBooking(UUID bookingId, UUID requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking não encontrado"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking já cancelado");
        }

        // RN-04: cancellation window — must cancel at least N hours before start
        long hoursUntilStart = ChronoUnit.HOURS.between(
            OffsetDateTime.now(ZoneOffset.UTC), booking.getSchedule().getStartsAt());
        if (hoursUntilStart < cancellationWindowHours) {
            throw new BusinessException(
                "Cancelamento não permitido com menos de " + cancellationWindowHours + "h de antecedência");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // RN-04 cascade: reopen schedule if it was FULL
        Schedule schedule = booking.getSchedule();
        if (schedule.getStatus() == ScheduleStatus.FULL) {
            schedule.setStatus(ScheduleStatus.OPEN);
            scheduleRepository.save(schedule);
        }
        bookingRepository.save(booking);
    }

    public void confirmBooking(UUID bookingId, UUID trainerId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking não encontrado"));
        Schedule schedule = booking.getSchedule();
        if (schedule.getType() != ScheduleType.PERSONAL) {
            throw new BusinessException("Apenas sessões individuais requerem confirmação");
        }
        if (!schedule.getTrainerId().equals(trainerId)) {
            throw new BusinessException("Apenas o profissional responsável pode confirmar");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking não está pendente");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
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
            null, // trainerName resolved at presentation layer if needed
            b.getBookedAt() != null ? b.getBookedAt().toString() : null
        );
    }
}
