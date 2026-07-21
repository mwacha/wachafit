package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.UpdateGroupClassRequest;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.booking.BookingRepository;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupClassServiceTest {

    @Mock GroupClassRepository groupClassRepository;
    @Mock UserRepository userRepository;
    @Mock BookingRepository bookingRepository;
    private GroupClassService service;

    @BeforeEach
    void setUp() {
        service = new GroupClassService(groupClassRepository, userRepository, bookingRepository);
    }

    @Test
    void create_shouldReturnResponse_whenTrainerExists() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer Name", "t@example.com");

        GroupClass gc = buildGroupClass(UUID.randomUUID(), "Funcional", 10, 60, trainer, true);

        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(groupClassRepository.save(any())).thenReturn(gc);

        GroupClassResponse res = service.create(
            new CreateGroupClassRequest("Funcional", null, 10, 60, trainerId, "FLEX", null, null, null));

        assertThat(res.name()).isEqualTo("Funcional");
        assertThat(res.trainerName()).isEqualTo("Trainer Name");
    }

    @Test
    void create_shouldThrow_whenTrainerNotFound() {
        UUID trainerId = UUID.randomUUID();
        when(userRepository.findById(trainerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
            new CreateGroupClassRequest("Funcional", null, 10, 60, trainerId, "FLEX", null, null, null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_shouldReturnAll_whenActiveIsNull() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer", "trainer@test.com");
        GroupClass gc = buildGroupClass(UUID.randomUUID(), "Yoga", 15, 45, trainer, true);

        when(groupClassRepository.findAll()).thenReturn(List.of(gc));

        List<GroupClassResponse> result = service.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Yoga");
    }

    @Test
    void list_shouldFilterByActive_whenActiveIsTrue() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer", "trainer@test.com");
        GroupClass gc = buildGroupClass(UUID.randomUUID(), "CrossFit", 20, 50, trainer, true);

        when(groupClassRepository.findByActive(true)).thenReturn(List.of(gc));

        List<GroupClassResponse> result = service.list(true);

        assertThat(result).hasSize(1);
        verify(groupClassRepository).findByActive(true);
        verify(groupClassRepository, never()).findAll();
    }

    @Test
    void update_shouldReturnUpdatedResponse_whenAdminUpdates() {
        UUID id = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "New Trainer", "new@test.com");
        GroupClass gc = buildGroupClass(id, "Original", 8, 30, trainer, true);

        when(groupClassRepository.findById(id)).thenReturn(Optional.of(gc));
        when(groupClassRepository.save(any())).thenReturn(gc);

        GroupClassResponse res = service.updateGroupClass(id,
            new UpdateGroupClassRequest("Updated", "desc", 8, 30, "FLEX", null, null, null),
            UUID.randomUUID(), Role.ADMIN);

        assertThat(res.name()).isEqualTo("Updated");
        assertThat(res.trainerName()).isEqualTo("New Trainer");
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(groupClassRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateGroupClass(id,
            new UpdateGroupClassRequest("X", null, 5, 30, "FLEX", null, null, null),
            UUID.randomUUID(), Role.ADMIN))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_shouldAllow_whenTrainerOwnsClass() {
        UUID trainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Owning Trainer", "owner@test.com");
        GroupClass gc = buildGroupClass(classId, "Pilates", 10, 45, trainer, true);

        when(groupClassRepository.findById(classId)).thenReturn(Optional.of(gc));
        when(groupClassRepository.save(any())).thenReturn(gc);

        GroupClassResponse res = service.updateGroupClass(classId,
            new UpdateGroupClassRequest("Pilates Updated", null, 10, 45, "FLEX", null, null, null),
            trainerId, Role.TRAINER);

        assertThat(res.name()).isEqualTo("Pilates Updated");
    }

    @Test
    void update_shouldThrowForbidden_whenTrainerDoesNotOwnClass() {
        UUID ownerId = UUID.randomUUID();
        UUID otherTrainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User owner = buildTrainer(ownerId, "Owner", "owner@test.com");
        GroupClass gc = buildGroupClass(classId, "Yoga", 10, 60, owner, true);

        when(groupClassRepository.findById(classId)).thenReturn(Optional.of(gc));

        assertThatThrownBy(() -> service.updateGroupClass(classId,
            new UpdateGroupClassRequest("Yoga Hacked", null, 10, 60, "FLEX", null, null, null),
            otherTrainerId, Role.TRAINER))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivate_shouldSetActiveFalse_whenAdminDeactivates() {
        UUID id = UUID.randomUUID();
        User trainer = buildTrainer(UUID.randomUUID(), "T", "t@t.com");
        GroupClass gc = buildGroupClass(id, "Pilates", 5, 60, trainer, true);

        when(groupClassRepository.findById(id)).thenReturn(Optional.of(gc));
        when(groupClassRepository.save(any())).thenReturn(gc);

        service.deactivateGroupClass(id, UUID.randomUUID(), Role.ADMIN);

        assertThat(gc.isActive()).isFalse();
        verify(groupClassRepository).save(gc);
    }

    @Test
    void deactivate_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(groupClassRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivateGroupClass(id, UUID.randomUUID(), Role.ADMIN))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivate_shouldAllow_whenTrainerOwnsClass() {
        UUID trainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Owner", "owner@test.com");
        GroupClass gc = buildGroupClass(classId, "Spinning", 12, 50, trainer, true);

        when(groupClassRepository.findById(classId)).thenReturn(Optional.of(gc));
        when(groupClassRepository.save(any())).thenReturn(gc);

        service.deactivateGroupClass(classId, trainerId, Role.TRAINER);

        assertThat(gc.isActive()).isFalse();
    }

    @Test
    void deactivate_shouldThrowForbidden_whenTrainerDoesNotOwnClass() {
        UUID ownerId = UUID.randomUUID();
        UUID otherTrainerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User owner = buildTrainer(ownerId, "Owner", "owner@test.com");
        GroupClass gc = buildGroupClass(classId, "Boxing", 8, 60, owner, true);

        when(groupClassRepository.findById(classId)).thenReturn(Optional.of(gc));

        assertThatThrownBy(() -> service.deactivateGroupClass(classId, otherTrainerId, Role.TRAINER))
            .isInstanceOf(ForbiddenException.class);
    }

    // --- helpers ---

    private User buildTrainer(UUID id, String name, String email) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }

    private GroupClass buildGroupClass(UUID id, String name, int capacity, int durationMinutes,
                                       User trainer, boolean active) {
        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName(name);
        gc.setCapacity(capacity);
        gc.setDurationMinutes(durationMinutes);
        gc.setTrainer(trainer);
        gc.setActive(active);
        return gc;
    }
}
