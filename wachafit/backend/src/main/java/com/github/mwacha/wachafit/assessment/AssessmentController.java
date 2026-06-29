package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
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
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/assessments")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public ResponseEntity<AssessmentResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateAssessmentRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/assessments")
    @PreAuthorize("isAuthenticated()")
    public List<AssessmentResponse> list(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @GetMapping("/api/students/{studentId}/assessments/evolution")
    @PreAuthorize("isAuthenticated()")
    public List<EvolutionPoint> evolution(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.evolution(studentId, currentUser);
    }

    @PutMapping("/api/assessments/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    public AssessmentResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssessmentRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.update(id, req, currentUser);
    }
}
