package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.EnrolledClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.UpdateGroupClassRequest;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class GroupClassService {

    private final GroupClassRepository groupClassRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public GroupClassService(GroupClassRepository groupClassRepository,
                             UserRepository userRepository,
                             ClassEnrollmentRepository enrollmentRepository) {
        this.groupClassRepository = groupClassRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EnrolledClassResponse> getStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findByStudentIdAndStatus(studentId, "ACTIVE").stream()
            .map(e -> {
                GroupClass gc = e.getGroupClass();
                String raw = gc.getDaysOfWeek();
                List<String> days = (raw != null && !raw.isBlank())
                    ? Arrays.asList(raw.split(","))
                    : List.of();
                return new EnrolledClassResponse(
                    gc.getId().toString(),
                    gc.getName(),
                    gc.getTrainer().getName(),
                    gc.getStartTime(),
                    gc.getEndTime(),
                    days
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupClassResponse> list(Boolean active) {
        List<GroupClass> classes = active != null
            ? groupClassRepository.findByActive(active)
            : groupClassRepository.findAll();
        return classes.stream().map(this::toResponse).toList();
    }

    public GroupClassResponse create(CreateGroupClassRequest req) {
        User trainer = findTrainer(req.trainerId());
        GroupClass gc = new GroupClass();
        gc.setName(req.name());
        gc.setDescription(req.description());
        gc.setCapacity(req.capacity());
        gc.setScheduleType(req.scheduleType() != null ? req.scheduleType() : "FLEX");
        gc.setStartTime(req.startTime());
        gc.setEndTime(req.endTime());
        gc.setDurationMinutes(resolveDuration(req.scheduleType(), req.startTime(), req.endTime(), req.durationMinutes()));
        gc.setDaysOfWeek(toDaysString(req.daysOfWeek()));
        gc.setTrainer(trainer);
        return toResponse(groupClassRepository.save(gc));
    }

    public GroupClassResponse updateGroupClass(UUID id, UpdateGroupClassRequest req,
                                               UUID currentUserId, Role currentUserRole) {
        GroupClass gc = findOrThrow(id);
        if (currentUserRole == Role.TRAINER && !gc.getTrainer().getId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        gc.setName(req.name());
        gc.setDescription(req.description());
        gc.setCapacity(req.capacity());
        gc.setScheduleType(req.scheduleType() != null ? req.scheduleType() : "FLEX");
        gc.setStartTime(req.startTime());
        gc.setEndTime(req.endTime());
        gc.setDurationMinutes(resolveDuration(req.scheduleType(), req.startTime(), req.endTime(), req.durationMinutes()));
        gc.setDaysOfWeek(toDaysString(req.daysOfWeek()));
        if (req.trainerId() != null) {
            gc.setTrainer(findTrainer(req.trainerId()));
        }
        return toResponse(groupClassRepository.save(gc));
    }

    public GroupClassResponse reactivateGroupClass(UUID id) {
        GroupClass gc = findOrThrow(id);
        if (gc.isActive()) {
            throw new BusinessException("Turma já está ativa");
        }
        if ("FIXED".equals(gc.getScheduleType()) && gc.getStartTime() != null && gc.getEndTime() != null) {
            List<GroupClass> conflicts = groupClassRepository.findActiveFixedConflicts(
                id, gc.getStartTime(), gc.getEndTime());
            if (gc.getDaysOfWeek() != null && !gc.getDaysOfWeek().isBlank()) {
                Set<String> days = new HashSet<>(Arrays.asList(gc.getDaysOfWeek().split(",")));
                for (GroupClass other : conflicts) {
                    if (other.getDaysOfWeek() == null || other.getDaysOfWeek().isBlank()) continue;
                    Set<String> otherDays = new HashSet<>(Arrays.asList(other.getDaysOfWeek().split(",")));
                    otherDays.retainAll(days);
                    if (!otherDays.isEmpty()) {
                        throw new BusinessException(
                            "Conflito com a turma \"" + other.getName() + "\" que já está ativa no mesmo horário e dia(s)");
                    }
                }
            } else if (!conflicts.isEmpty()) {
                throw new BusinessException(
                    "Conflito com a turma \"" + conflicts.get(0).getName() + "\" que já está ativa no mesmo horário");
            }
        }
        gc.setActive(true);
        return toResponse(groupClassRepository.save(gc));
    }

    public void deactivateGroupClass(UUID id, UUID currentUserId, Role currentUserRole) {
        GroupClass gc = findOrThrow(id);
        if (currentUserRole == Role.TRAINER && !gc.getTrainer().getId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        gc.setActive(false);
        groupClassRepository.save(gc);
    }

    private String toDaysString(List<String> days) {
        if (days == null || days.isEmpty()) return null;
        return String.join(",", days);
    }

    private int resolveDuration(String scheduleType, String startTime, String endTime, Integer durationMinutes) {
        if ("FIXED".equals(scheduleType) && startTime != null && endTime != null) {
            try {
                String[] s = startTime.split(":");
                String[] e = endTime.split(":");
                int startMin = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
                int endMin   = Integer.parseInt(e[0]) * 60 + Integer.parseInt(e[1]);
                int diff = endMin - startMin;
                return diff > 0 ? diff : diff + 24 * 60;
            } catch (Exception ex) {
                return durationMinutes != null ? durationMinutes : 60;
            }
        }
        return durationMinutes != null ? durationMinutes : 60;
    }

    private GroupClass findOrThrow(UUID id) {
        return groupClassRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Turma não encontrada: " + id));
    }

    private User findTrainer(UUID trainerId) {
        return userRepository.findById(trainerId)
            .orElseThrow(() -> new NotFoundException("Profissional não encontrado: " + trainerId));
    }

    private GroupClassResponse toResponse(GroupClass gc) {
        int enrolled = (int) enrollmentRepository.countByGroupClassIdAndStatus(gc.getId(), "ACTIVE");
        String raw = gc.getDaysOfWeek();
        List<String> days = (raw != null && !raw.isBlank())
            ? Arrays.asList(raw.split(","))
            : null;
        return new GroupClassResponse(
            gc.getId().toString(),
            gc.getName(),
            gc.getDescription(),
            gc.getCapacity(),
            gc.getDurationMinutes(),
            gc.getTrainer().getId().toString(),
            gc.getTrainer().getName(),
            gc.isActive(),
            gc.getCreatedAt() != null ? gc.getCreatedAt().toString() : null,
            gc.getScheduleType(),
            gc.getStartTime(),
            gc.getEndTime(),
            days,
            enrolled
        );
    }
}
