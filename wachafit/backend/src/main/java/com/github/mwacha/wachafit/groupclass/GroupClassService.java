package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.UpdateGroupClassRequest;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GroupClassService {

    private final GroupClassRepository groupClassRepository;
    private final UserRepository userRepository;

    public GroupClassService(GroupClassRepository groupClassRepository, UserRepository userRepository) {
        this.groupClassRepository = groupClassRepository;
        this.userRepository = userRepository;
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
        gc.setDurationMinutes(req.durationMinutes());
        gc.setTrainer(trainer);
        return toResponse(groupClassRepository.save(gc));
    }

    /** Legacy overload kept for service tests that still use GroupClassRequest. */
    public GroupClassResponse create(GroupClassRequest req) {
        User trainer = findTrainer(req.trainerId());
        GroupClass gc = new GroupClass();
        gc.setName(req.name());
        gc.setDescription(req.description());
        gc.setCapacity(req.capacity());
        gc.setDurationMinutes(req.durationMinutes());
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
        gc.setDurationMinutes(req.durationMinutes());
        return toResponse(groupClassRepository.save(gc));
    }

    /** Legacy overload kept for service tests that still use GroupClassRequest. */
    public GroupClassResponse update(UUID id, GroupClassRequest req) {
        GroupClass gc = findOrThrow(id);
        User trainer = findTrainer(req.trainerId());
        gc.setName(req.name());
        gc.setDescription(req.description());
        gc.setCapacity(req.capacity());
        gc.setDurationMinutes(req.durationMinutes());
        gc.setTrainer(trainer);
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

    /** Legacy overload kept for service tests. */
    public void deactivate(UUID id) {
        GroupClass gc = findOrThrow(id);
        gc.setActive(false);
        groupClassRepository.save(gc);
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
        return new GroupClassResponse(
            gc.getId().toString(),
            gc.getName(),
            gc.getDescription(),
            gc.getCapacity(),
            gc.getDurationMinutes(),
            gc.getTrainer().getId().toString(),
            gc.getTrainer().getName(),
            gc.isActive(),
            gc.getCreatedAt() != null ? gc.getCreatedAt().toString() : null
        );
    }
}
