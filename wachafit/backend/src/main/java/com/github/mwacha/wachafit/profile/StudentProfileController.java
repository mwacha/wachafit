package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class StudentProfileController {

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) { this.service = service; }

    @PostMapping("/api/students/{studentId}/profile")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN','MANAGER')")
    public ResponseEntity<StudentProfileResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateStudentProfileRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createProfile(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/profile")
    @PreAuthorize("isAuthenticated()")
    public StudentProfileResponse get(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.getProfile(studentId, currentUser);
    }

    @PutMapping("/api/students/{studentId}/profile")
    @PreAuthorize("isAuthenticated()")
    public StudentProfileResponse update(@PathVariable UUID studentId,
            @Valid @RequestBody CreateStudentProfileRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.updateProfile(studentId, req, currentUser);
    }

    @PostMapping("/api/students/{studentId}/health")
    @PreAuthorize("isAuthenticated()")
    public StudentHealthResponse upsertHealth(@PathVariable UUID studentId,
            @Valid @RequestBody StudentHealthRequest req,
            @AuthenticationPrincipal User currentUser) {
        return service.upsertHealth(studentId, req, currentUser);
    }

    @GetMapping("/api/students/{studentId}/health")
    @PreAuthorize("isAuthenticated()")
    public StudentHealthResponse getHealth(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.getHealth(studentId, currentUser);
    }
}
