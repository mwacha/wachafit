package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.UpdateGroupClassRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
public class GroupClassController {

    private final GroupClassService groupClassService;

    public GroupClassController(GroupClassService groupClassService) {
        this.groupClassService = groupClassService;
    }

    @GetMapping
    public ResponseEntity<List<GroupClassResponse>> list(
        @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(groupClassService.list(active));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<GroupClassResponse> create(
        @Valid @RequestBody CreateGroupClassRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupClassService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<GroupClassResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateGroupClassRequest req,
        @AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser
    ) {
        return ResponseEntity.ok(
            groupClassService.updateGroupClass(id, req, currentUser.getId(), currentUser.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<Void> deactivate(
        @PathVariable UUID id,
        @AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser
    ) {
        groupClassService.deactivateGroupClass(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }
}
