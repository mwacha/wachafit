package com.github.mwacha.wachafit.goal;

import com.github.mwacha.wachafit.goal.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/goals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GoalResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateGoalRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/goals")
    @PreAuthorize("isAuthenticated()")
    public List<GoalResponse> list(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @PatchMapping("/api/goals/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public GoalResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoalStatusRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.updateStatus(id, req, currentUser);
    }

    @DeleteMapping("/api/goals/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
