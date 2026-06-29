package com.github.mwacha.wachafit.exercise;

import com.github.mwacha.wachafit.exercise.dto.*;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock ExerciseRepository repo;
    @InjectMocks ExerciseService service;

    @Test
    void create_shouldPersistAndReturnResponse() {
        Exercise saved = new Exercise();
        saved.setName("Squat");
        saved.setMuscleGroup("legs");
        saved.setActive(true);

        when(repo.save(any(Exercise.class))).thenReturn(saved);

        ExerciseResponse res = service.create(
                new CreateExerciseRequest("Squat", "legs", null, null));

        assertThat(res.name()).isEqualTo("Squat");
        assertThat(res.muscleGroup()).isEqualTo("legs");
        assertThat(res.active()).isTrue();
        verify(repo).save(any(Exercise.class));
    }

    @Test
    void search_shouldDelegateToRepository() {
        Exercise e = new Exercise();
        e.setName("Bench Press");
        e.setMuscleGroup("chest");
        e.setActive(true);

        when(repo.search(null, null)).thenReturn(List.of(e));

        List<ExerciseResponse> results = service.search(null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Bench Press");
    }

    @Test
    void update_shouldModifyFieldsAndSave() {
        UUID id = UUID.randomUUID();
        Exercise existing = new Exercise();
        existing.setName("Old Name");
        existing.setMuscleGroup("back");
        existing.setActive(true);

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseResponse res = service.update(id,
                new UpdateExerciseRequest("New Name", "chest", "desc", "http://video.url"));

        assertThat(res.name()).isEqualTo("New Name");
        assertThat(res.muscleGroup()).isEqualTo("chest");
        assertThat(res.description()).isEqualTo("desc");
        assertThat(res.videoUrl()).isEqualTo("http://video.url");
        verify(repo).save(existing);
    }

    @Test
    void update_shouldThrowNotFound_whenExerciseDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new UpdateExerciseRequest("X", "Y", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Exercise not found");
    }

    @Test
    void deactivate_shouldSetActiveFalseAndSave() {
        UUID id = UUID.randomUUID();
        Exercise existing = new Exercise();
        existing.setName("Pull-up");
        existing.setMuscleGroup("back");
        existing.setActive(true);

        when(repo.findById(id)).thenReturn(Optional.of(existing));

        service.deactivate(id);

        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
    }

    @Test
    void deactivate_shouldThrowNotFound_whenExerciseDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Exercise not found");
    }
}
