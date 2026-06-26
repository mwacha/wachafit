package com.github.mwacha.wachafit.schedule;

import com.github.mwacha.wachafit.groupclass.GroupClass;
import com.github.mwacha.wachafit.groupclass.GroupClassRepository;
import com.github.mwacha.wachafit.schedule.dto.*;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TrainerAvailabilityRepository availabilityRepository;
    private final GroupClassRepository groupClassRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           TrainerAvailabilityRepository availabilityRepository,
                           GroupClassRepository groupClassRepository) {
        this.scheduleRepository = scheduleRepository;
        this.availabilityRepository = availabilityRepository;
        this.groupClassRepository = groupClassRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(OffsetDateTime from, OffsetDateTime to, UUID trainerId, ScheduleType type) {
        return scheduleRepository.findByFilters(from, to, trainerId, type == null ? null : type.name())
            .stream().map(this::toResponse).toList();
    }

    public ScheduleResponse create(ScheduleRequest req) {
        validateNoOverlap(req.trainerId(), req.startsAt(), req.endsAt());
        Schedule s = buildSchedule(req.groupClassId(), req.trainerId(), req.type(), req.startsAt(), req.endsAt());
        return toResponse(scheduleRepository.save(s));
    }

    public List<ScheduleResponse> createBatch(BatchScheduleRequest req) {
        // Validate ALL slots first — fail fast before persisting any
        List<String> conflicts = new ArrayList<>();
        for (SlotRequest slot : req.slots()) {
            long count = scheduleRepository.countOverlaps(req.trainerId(), slot.startsAt(), slot.endsAt());
            if (count > 0) {
                conflicts.add(slot.startsAt() + " - " + slot.endsAt());
            }
        }
        if (!conflicts.isEmpty()) {
            throw new BusinessException("Conflito de horário do profissional nos slots: " + String.join(", ", conflicts));
        }
        return req.slots().stream()
            .map(slot -> buildSchedule(req.groupClassId(), req.trainerId(), req.type(), slot.startsAt(), slot.endsAt()))
            .map(scheduleRepository::save)
            .map(this::toResponse)
            .toList();
    }

    public void cancel(UUID id, UUID requestingUserId) {
        Schedule s = scheduleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Schedule não encontrado: " + id));
        if (s.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException("Schedule já cancelado");
        }
        s.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepository.save(s);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID trainerId) {
        List<AvailabilityResponse.SlotAvailability> slots = availabilityRepository
            .findByTrainerId(trainerId).stream()
            .map(a -> new AvailabilityResponse.SlotAvailability(
                a.getWeekday(), a.getStartTime().toString(), a.getEndTime().toString()))
            .toList();
        return new AvailabilityResponse(slots);
    }

    public AvailabilityResponse setAvailability(UUID trainerId, AvailabilityRequest req) {
        availabilityRepository.deleteByTrainerId(trainerId);
        List<TrainerAvailability> saved = req.slots().stream().map(slot -> {
            TrainerAvailability a = new TrainerAvailability();
            a.setTrainerId(trainerId);
            a.setWeekday(slot.weekday());
            a.setStartTime(LocalTime.parse(slot.startTime()));
            a.setEndTime(LocalTime.parse(slot.endTime()));
            return availabilityRepository.save(a);
        }).toList();
        List<AvailabilityResponse.SlotAvailability> slots = saved.stream()
            .map(a -> new AvailabilityResponse.SlotAvailability(
                a.getWeekday(), a.getStartTime().toString(), a.getEndTime().toString()))
            .toList();
        return new AvailabilityResponse(slots);
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
