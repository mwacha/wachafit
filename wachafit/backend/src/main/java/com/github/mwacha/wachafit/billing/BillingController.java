package com.github.mwacha.wachafit.billing;

import com.github.mwacha.wachafit.billing.dto.ChargeResponse;
import com.github.mwacha.wachafit.billing.dto.CreateChargeRequest;
import com.github.mwacha.wachafit.billing.dto.ManualPaymentRequest;
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
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/api/students/{studentId}/charges")
    @PreAuthorize("isAuthenticated()")
    public List<ChargeResponse> listCharges(@PathVariable UUID studentId,
                                            @AuthenticationPrincipal User currentUser) {
        return service.listCharges(studentId, currentUser);
    }

    @PostMapping("/api/students/{studentId}/charges")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ChargeResponse> createManualCharge(
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateChargeRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.createManualCharge(studentId, req, currentUser));
    }

    @PatchMapping("/api/charges/{id}/pay")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','CASHIER','ADMIN','MANAGER')")
    public ChargeResponse payCharge(@PathVariable UUID id,
                                    @Valid @RequestBody ManualPaymentRequest req) {
        return service.payCharge(id, req);
    }

    @PatchMapping("/api/charges/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelCharge(@PathVariable UUID id) {
        service.cancelCharge(id);
    }
}
