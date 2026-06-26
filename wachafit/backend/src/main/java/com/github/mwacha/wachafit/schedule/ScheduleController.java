package com.github.mwacha.wachafit.schedule;

import com.github.mwacha.wachafit.schedule.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/api/schedules")
    public ResponseEntity<List<ScheduleResponse>> list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(required = false) UUID trainerId,
        @RequestParam(required = false) ScheduleType type
    ) {
        return ResponseEntity.ok(scheduleService.list(from, to, trainerId, type));
    }

    @PostMapping("/api/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ScheduleResponse> create(
        @Valid @RequestBody ScheduleRequest req,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(scheduleService.create(req));
    }

    @PostMapping("/api/schedules/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleResponse>> createBatch(@Valid @RequestBody BatchScheduleRequest req) {
        return ResponseEntity.ok(scheduleService.createBatch(req));
    }

    @PatchMapping("/api/schedules/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Void> cancel(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser
    ) {
        scheduleService.cancel(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/trainers/{id}/availability")
    public ResponseEntity<AvailabilityResponse> getAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.getAvailability(id));
    }

    @PutMapping("/api/trainers/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<AvailabilityResponse> setAvailability(
        @PathVariable UUID id,
        @Valid @RequestBody AvailabilityRequest req
    ) {
        return ResponseEntity.ok(scheduleService.setAvailability(id, req));
    }
}
