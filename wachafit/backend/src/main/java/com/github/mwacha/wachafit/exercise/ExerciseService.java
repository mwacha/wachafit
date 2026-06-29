package com.github.mwacha.wachafit.exercise;

import com.github.mwacha.wachafit.exercise.dto.*;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ExerciseService {

    private final ExerciseRepository repo;

    public ExerciseService(ExerciseRepository repo) {
        this.repo = repo;
    }

    public List<ExerciseResponse> search(String q, String muscleGroup) {
        return repo.search(q, muscleGroup).stream()
                .map(this::toResponse)
                .toList();
    }

    public ExerciseResponse create(CreateExerciseRequest req) {
        Exercise e = new Exercise();
        e.setName(req.name());
        e.setMuscleGroup(req.muscleGroup());
        e.setDescription(req.description());
        e.setVideoUrl(req.videoUrl());
        return toResponse(repo.save(e));
    }

    public ExerciseResponse update(UUID id, UpdateExerciseRequest req) {
        Exercise e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        e.setName(req.name());
        e.setMuscleGroup(req.muscleGroup());
        e.setDescription(req.description());
        e.setVideoUrl(req.videoUrl());
        return toResponse(repo.save(e));
    }

    public void deactivate(UUID id) {
        Exercise e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        e.setActive(false);
        repo.save(e);
    }

    private ExerciseResponse toResponse(Exercise e) {
        return new ExerciseResponse(
                e.getId(), e.getName(), e.getMuscleGroup(),
                e.getDescription(), e.getVideoUrl(), e.isActive());
    }
}
