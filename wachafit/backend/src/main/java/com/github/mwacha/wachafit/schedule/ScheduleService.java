package com.github.mwacha.wachafit.schedule;

import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.schedule.dto.*;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final GroupClassRepository groupClassRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           GroupClassRepository groupClassRepository) {
        this.scheduleRepository = scheduleRepository;
        this.groupClassRepository = groupClassRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(OffsetDateTime from, OffsetDateTime to, LocalDate date, UUID trainerId, ScheduleType type) {
        return scheduleRepository.findByFilters(from, to, date, trainerId, type == null ? null : type.name())
            .stream().map(this::toResponse).toList();
    }

    public ScheduleResponse create(ScheduleRequest req) {
        validateNoOverlap(req.trainerId(), req.startsAt(), req.endsAt());
        Schedule s = buildSchedule(req.groupClassId(), req.trainerId(), req.type(), req.startsAt(), req.endsAt());
        return toResponse(scheduleRepository.save(s));
    }

    public void cancel(UUID id, UUID currentUserId, Role currentUserRole) {
        Schedule s = scheduleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado: " + id));
        if (currentUserRole == Role.TRAINER && !s.getTrainerId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException("Schedule já cancelado");
        }
        s.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepository.save(s);
    }

    private void validateNoOverlap(UUID trainerId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        long count = scheduleRepository.countOverlaps(trainerId, startsAt, endsAt);
        if (count > 0) {
            throw new BusinessException("Profissional já tem um compromisso neste horário (conflito)");
        }
    }

    private Schedule buildSchedule(UUID groupClassId, UUID trainerId, ScheduleType type,
                                   OffsetDateTime startsAt, OffsetDateTime endsAt) {
        Schedule s = new Schedule();
        if (groupClassId != null) {
            GroupClass gc = groupClassRepository.findById(groupClassId)
                .orElseThrow(() -> new NotFoundException("Turma não encontrada: " + groupClassId));
            s.setGroupClass(gc);
        }
        s.setTrainerId(trainerId);
        s.setType(type);
        s.setStartsAt(startsAt);
        s.setEndsAt(endsAt);
        return s;
    }

    private ScheduleResponse toResponse(Schedule s) {
        return new ScheduleResponse(
            s.getId().toString(),
            s.getGroupClass() != null ? s.getGroupClass().getId().toString() : null,
            s.getGroupClass() != null ? s.getGroupClass().getName() : null,
            s.getTrainerId().toString(),
            s.getType().name(), s.getStatus().name(),
            s.getStartsAt().toString(), s.getEndsAt().toString(),
            s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }
}
