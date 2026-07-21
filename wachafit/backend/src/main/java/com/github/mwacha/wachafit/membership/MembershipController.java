package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MembershipController {

    private final MembershipService service;

    public MembershipController(MembershipService service) {
        this.service = service;
    }

    @PostMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN','MANAGER')")
    public ResponseEntity<SubscriptionResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateSubscriptionRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createSubscription(studentId, req, currentUser.getId()));
    }

    @GetMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionResponse> getActive(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        SubscriptionResponse res = service.getActiveSubscription(studentId, currentUser);
        return res != null ? ResponseEntity.ok(res) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/api/students/{studentId}/subscription")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable UUID studentId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User currentUser) {
        service.cancelSubscription(studentId, reason, currentUser);
    }
}
