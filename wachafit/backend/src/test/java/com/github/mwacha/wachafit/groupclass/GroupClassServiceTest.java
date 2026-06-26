package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.GroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.User;
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
    private GroupClassService service;

    @BeforeEach
    void setUp() {
        service = new GroupClassService(groupClassRepository, userRepository);
    }

    @Test
    void create_shouldReturnResponse_whenTrainerExists() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer Name", "t@example.com");

        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName("Funcional");
        gc.setCapacity(10);
        gc.setDurationMinutes(60);
        gc.setTrainer(trainer);
        gc.setActive(true);

        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(groupClassRepository.save(any())).thenReturn(gc);

        GroupClassResponse res = service.create(
            new GroupClassRequest("Funcional", null, 10, 60, trainerId));

        assertThat(res.name()).isEqualTo("Funcional");
        assertThat(res.trainerName()).isEqualTo("Trainer Name");
    }

    @Test
    void create_shouldThrow_whenTrainerNotFound() {
        UUID trainerId = UUID.randomUUID();
        when(userRepository.findById(trainerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
            new GroupClassRequest("Funcional", null, 10, 60, trainerId)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_shouldReturnAll_whenActiveIsNull() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer", "trainer@test.com");

        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName("Yoga"); gc.setCapacity(15); gc.setDurationMinutes(45);
        gc.setTrainer(trainer); gc.setActive(true);

        when(groupClassRepository.findAll()).thenReturn(List.of(gc));

        List<GroupClassResponse> result = service.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Yoga");
    }

    @Test
    void list_shouldFilterByActive_whenActiveIsTrue() {
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "Trainer", "trainer@test.com");

        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName("CrossFit"); gc.setCapacity(20); gc.setDurationMinutes(50);
        gc.setTrainer(trainer); gc.setActive(true);

        when(groupClassRepository.findByActive(true)).thenReturn(List.of(gc));

        List<GroupClassResponse> result = service.list(true);

        assertThat(result).hasSize(1);
        verify(groupClassRepository).findByActive(true);
        verify(groupClassRepository, never()).findAll();
    }

    @Test
    void update_shouldReturnUpdatedResponse_whenExists() {
        UUID id = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        User trainer = buildTrainer(trainerId, "New Trainer", "new@test.com");

        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName("Updated"); gc.setCapacity(8); gc.setDurationMinutes(30);
        gc.setTrainer(trainer); gc.setActive(true);

        when(groupClassRepository.findById(id)).thenReturn(Optional.of(gc));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(groupClassRepository.save(any())).thenReturn(gc);

        GroupClassResponse res = service.update(id,
            new GroupClassRequest("Updated", "desc", 8, 30, trainerId));

        assertThat(res.name()).isEqualTo("Updated");
        assertThat(res.trainerName()).isEqualTo("New Trainer");
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        when(groupClassRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id,
            new GroupClassRequest("X", null, 5, 30, trainerId)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivate_shouldSetActiveFalse_whenExists() {
        UUID id = UUID.randomUUID();
        User trainer = buildTrainer(UUID.randomUUID(), "T", "t@t.com");

        GroupClass gc = new GroupClass();
        try {
            var f = GroupClass.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        gc.setName("Pilates"); gc.setCapacity(5); gc.setDurationMinutes(60);
        gc.setTrainer(trainer); gc.setActive(true);

        when(groupClassRepository.findById(id)).thenReturn(Optional.of(gc));
        when(groupClassRepository.save(any())).thenReturn(gc);

        service.deactivate(id);

        assertThat(gc.isActive()).isFalse();
        verify(groupClassRepository).save(gc);
    }

    @Test
    void deactivate_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(groupClassRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate(id))
            .isInstanceOf(NotFoundException.class);
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
}
