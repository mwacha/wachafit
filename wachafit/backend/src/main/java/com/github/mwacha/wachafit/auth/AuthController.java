package com.github.mwacha.wachafit.auth;

import com.github.mwacha.wachafit.auth.dto.*;
import com.github.mwacha.wachafit.user.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<LoginResponse> selectTenant(@Valid @RequestBody SelectTenantRequest request) {
        return ResponseEntity.ok(authService.selectTenant(request));
    }

    @PostMapping("/switch-tenant")
    public ResponseEntity<LoginResponse> switchTenant(
        @Valid @RequestBody SwitchTenantRequest request,
        @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(authService.switchTenant(request, currentUser));
    }

    @GetMapping("/my-tenants")
    public ResponseEntity<List<TenantMembershipSummary>> myTenants(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.myTenants(currentUser));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
