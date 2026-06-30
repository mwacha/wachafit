package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class TrainerProfileController {

    private final TrainerProfileService service;

    public TrainerProfileController(TrainerProfileService service) { this.service = service; }

    @PostMapping("/api/trainers/{trainerId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TrainerProfileResponse> upsert(
            @PathVariable UUID trainerId,
            @Valid @RequestBody CreateTrainerProfileRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.upsert(trainerId, req, currentUser));
    }

    @GetMapping("/api/trainers/{trainerId}/profile")
    @PreAuthorize("isAuthenticated()")
    public TrainerProfileResponse get(@PathVariable UUID trainerId) {
        return service.get(trainerId);
    }
}
