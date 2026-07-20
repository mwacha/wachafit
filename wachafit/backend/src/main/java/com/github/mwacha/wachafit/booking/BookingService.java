package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.groupclass.dto.EnrolledStudentResponse;
import com.github.mwacha.wachafit.notification.event.BookingCancelledEvent;
import com.github.mwacha.wachafit.notification.event.BookingConfirmedEvent;
import com.github.mwacha.wachafit.notification.event.PersonalSessionRequestedEvent;
import com.github.mwacha.wachafit.schedule.Schedule;
import com.github.mwacha.wachafit.schedule.ScheduleRepository;
import com.github.mwacha.wachafit.schedule.ScheduleStatus;
import com.github.mwacha.wachafit.schedule.ScheduleType;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          UserRepository userRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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

        long alreadyBooked = bookingRepository.countActiveByScheduleAndStudent(req.scheduleId(), studentId);
        if (alreadyBooked > 0) {
            throw new BusinessException("Você já possui uma reserva ativa para esta aula");
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

        String className = locked.getGroupClass() != null
            ? locked.getGroupClass().getName() : "Sessão individual";
        String date = locked.getStartsAt().toLocalDate().toString();
        String time = locked.getStartsAt().toLocalTime().toString();

        if (status == BookingStatus.CONFIRMED) {
            eventPublisher.publishEvent(new BookingConfirmedEvent(
                studentId, locked.getTrainerId(), className, date, time));
        } else {
            // PERSONAL session — notify trainer of the pending request
            eventPublisher.publishEvent(new PersonalSessionRequestedEvent(
                studentId, locked.getTrainerId(), date, time));
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

        eventPublisher.publishEvent(new BookingCancelledEvent(
            booking.getStudentId(),
            schedule.getGroupClass() != null ? schedule.getGroupClass().getName() : "Sessão individual",
            schedule.getStartsAt().toLocalDate().toString(),
            schedule.getStartsAt().toLocalTime().toString()
        ));
    }

    @Transactional(readOnly = true)
    public List<EnrolledStudentResponse> listEnrolledStudents(UUID classId) {
        List<Schedule> upcoming = scheduleRepository.findUpcomingByClassId(classId, OffsetDateTime.now());
        if (upcoming.isEmpty()) return List.of();

        List<UUID> scheduleIds = upcoming.stream().map(Schedule::getId).toList();
        List<Booking> bookings = bookingRepository.findActiveByScheduleIds(scheduleIds);

        Map<UUID, Long> countByStudent = bookings.stream()
            .collect(Collectors.groupingBy(Booking::getStudentId, Collectors.counting()));

        List<User> users = userRepository.findAllById(countByStudent.keySet());

        return users.stream()
            .map(u -> new EnrolledStudentResponse(
                u.getId().toString(),
                u.getName(),
                u.getEmail(),
                countByStudent.getOrDefault(u.getId(), 0L).intValue()
            ))
            .sorted(Comparator.comparing(EnrolledStudentResponse::name))
            .toList();
    }

    public void enrollStudentInClass(UUID classId, UUID studentId) {
        userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));

        List<Schedule> upcoming = scheduleRepository.findUpcomingByClassId(classId, OffsetDateTime.now());

        for (Schedule schedule : upcoming) {
            long existing = bookingRepository.countActiveByScheduleAndStudent(schedule.getId(), studentId);
            if (existing > 0) continue;

            int capacity = schedule.getGroupClass().getCapacity();
            long confirmed = bookingRepository.countConfirmedBookings(schedule.getId());
            if (confirmed >= capacity) continue;

            Booking booking = new Booking();
            booking.setSchedule(schedule);
            booking.setStudentId(studentId);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            if (confirmed + 1 >= capacity) {
                schedule.setStatus(ScheduleStatus.FULL);
                scheduleRepository.save(schedule);
            }
        }
    }

    public void unenrollStudentFromClass(UUID classId, UUID studentId) {
        List<Schedule> upcoming = scheduleRepository.findUpcomingByClassId(classId, OffsetDateTime.now());
        if (upcoming.isEmpty()) return;

        List<UUID> scheduleIds = upcoming.stream().map(Schedule::getId).toList();
        List<Booking> bookings = bookingRepository.findActiveByScheduleIdsAndStudentId(scheduleIds, studentId);

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            Schedule schedule = booking.getSchedule();
            if (schedule.getStatus() == ScheduleStatus.FULL) {
                schedule.setStatus(ScheduleStatus.OPEN);
                scheduleRepository.save(schedule);
            }
            bookingRepository.save(booking);
        }
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
