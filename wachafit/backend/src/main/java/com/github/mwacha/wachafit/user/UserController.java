package com.github.mwacha.wachafit.user;

import com.github.mwacha.wachafit.user.dto.CreateUserRequest;
import com.github.mwacha.wachafit.user.dto.UpdateUserRequest;
import com.github.mwacha.wachafit.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> list(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(userService.listUsers(role, active));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest req
    ) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(
        @PathVariable UUID id,
        @AuthenticationPrincipal User currentUser
    ) {
        userService.deactivateUser(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }
}
