package com.github.mwacha.wachafit.workout;

import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.workout.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/workout-plans")
    @PreAuthorize("hasAnyRole('TRAINER','PROFESSOR','ADMIN')")
    public ResponseEntity<WorkoutPlanResponse> createPlan(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateWorkoutPlanRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createPlan(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/workout-plans")
    @PreAuthorize("isAuthenticated()")
    public List<WorkoutPlanResponse> listPlans(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listPlans(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/workout-plans/active")
    @PreAuthorize("isAuthenticated()")
    public WorkoutPlanResponse getActivePlan(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.getActivePlan(studentId, currentUser);
    }

    @PutMapping("/api/workout-plans/{planId}")
    @PreAuthorize("hasAnyRole('TRAINER','PROFESSOR','ADMIN')")
    public WorkoutPlanResponse updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateWorkoutPlanRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.updatePlan(planId, req, currentUser);
    }

    @PatchMapping("/api/workout-plans/{planId}/activate")
    @PreAuthorize("hasAnyRole('TRAINER','PROFESSOR','ADMIN')")
    public WorkoutPlanResponse activatePlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal User currentUser) {
        return service.activatePlan(planId, currentUser);
    }

    @PostMapping("/api/students/{studentId}/workout-logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkoutLogResponse> createLog(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateWorkoutLogRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createLog(studentId, req, currentUser));
    }

    @GetMapping("/api/students/{studentId}/workout-logs")
    @PreAuthorize("isAuthenticated()")
    public List<WorkoutLogResponse> listLogs(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listLogs(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/records")
    @PreAuthorize("isAuthenticated()")
    public List<PersonalRecordResponse> listRecords(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.listRecords(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/progression/{exerciseId}")
    @PreAuthorize("isAuthenticated()")
    public List<ProgressionPoint> progression(
            @PathVariable UUID studentId,
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal User currentUser) {
        return service.progression(studentId, exerciseId, currentUser);
    }
}
