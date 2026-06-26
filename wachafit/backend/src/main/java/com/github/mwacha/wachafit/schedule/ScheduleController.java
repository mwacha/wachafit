package com.github.mwacha.wachafit.schedule;

import com.github.mwacha.wachafit.schedule.dto.*;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) UUID trainerId,
        @RequestParam(required = false) ScheduleType type
    ) {
        return ResponseEntity.ok(scheduleService.list(from, to, date, trainerId, type));
    }

    @PostMapping("/api/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ScheduleResponse> create(
        @Valid @RequestBody ScheduleRequest req,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(req));
    }

    @DeleteMapping("/api/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Void> cancel(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser
    ) {
        scheduleService.cancel(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}
