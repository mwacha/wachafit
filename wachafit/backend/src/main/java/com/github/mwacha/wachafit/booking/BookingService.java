package com.github.mwacha.wachafit.booking;

import com.github.mwacha.wachafit.booking.dto.BookingResponse;
import com.github.mwacha.wachafit.booking.dto.CreateBookingRequest;
import com.github.mwacha.wachafit.groupclass.ClassEnrollment;
import com.github.mwacha.wachafit.groupclass.ClassEnrollmentRepository;
import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.groupclass.dto.EnrolledStudentResponse;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import com.github.mwacha.wachafit.membership.MembershipPlanRepository;
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
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final GroupClassRepository groupClassRepository;
    private final UserRepository userRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          ScheduleRepository scheduleRepository,
                          ClassEnrollmentRepository enrollmentRepository,
                          GroupClassRepository groupClassRepository,
                          UserRepository userRepository,
                          MemberSubscriptionRepository memberSubscriptionRepository,
                          MembershipPlanRepository membershipPlanRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.groupClassRepository = groupClassRepository;
        this.userRepository = userRepository;
        this.memberSubscriptionRepository = memberSubscriptionRepository;
        this.membershipPlanRepository = membershipPlanRepository;
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

        BookingStatus status = BookingStatus.CONFIRMED;

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
        return bookingRepository.findPersonalByStudentIdOrderByBookedAtDesc(studentId)
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
        List<ClassEnrollment> enrollments = enrollmentRepository.findByGroupClassIdAndStatus(classId, "ACTIVE");
        if (enrollments.isEmpty()) return List.of();
        List<UUID> studentIds = enrollments.stream().map(ClassEnrollment::getStudentId).toList();
        List<User> users = userRepository.findAllById(studentIds);
        return users.stream()
            .map(u -> new EnrolledStudentResponse(u.getId().toString(), u.getName(), u.getEmail()))
            .sorted(Comparator.comparing(EnrolledStudentResponse::name))
            .toList();
    }

    public void enrollStudentInClass(UUID classId, UUID studentId) {
        if (!userRepository.existsByIdAndTenantId(studentId, TenantContext.get())) {
            throw new NotFoundException("Aluno não encontrado");
        }

        enrollmentRepository.findByGroupClassIdAndStudentId(classId, studentId)
            .filter(e -> "ACTIVE".equals(e.getStatus()))
            .ifPresent(e -> { throw new BusinessException("Aluno já está inscrito nesta turma"); });

        var subscription = memberSubscriptionRepository.findByStudentIdAndStatus(studentId, "ACTIVE")
            .orElseThrow(() -> new BusinessException("Aluno sem plano ativo. Associe um plano antes de inscrever em turmas."));
        var plan = membershipPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));

        int maxClasses = plan.getMaxClassesPerWeek() != null ? plan.getMaxClassesPerWeek() : Integer.MAX_VALUE;
        long currentClasses = enrollmentRepository.countByStudentIdAndStatus(studentId, "ACTIVE");
        if (currentClasses >= maxClasses) {
            throw new BusinessException(
                "Limite do plano atingido: aluno já está em " + currentClasses + " de " + maxClasses + " turma(s) permitida(s)"
            );
        }

        GroupClass gc = groupClassRepository.findById(classId)
            .orElseThrow(() -> new NotFoundException("Turma não encontrada"));

        enrollmentRepository.findByGroupClassIdAndStudentId(classId, studentId)
            .ifPresentOrElse(
                e -> { e.setStatus("ACTIVE"); enrollmentRepository.save(e); },
                () -> {
                    ClassEnrollment e = new ClassEnrollment();
                    e.setGroupClass(gc);
                    e.setStudentId(studentId);
                    enrollmentRepository.save(e);
                }
            );
    }

    public void unenrollStudentFromClass(UUID classId, UUID studentId) {
        enrollmentRepository.findByGroupClassIdAndStudentId(classId, studentId)
            .filter(e -> "ACTIVE".equals(e.getStatus()))
            .ifPresent(e -> { e.setStatus("CANCELLED"); enrollmentRepository.save(e); });
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
