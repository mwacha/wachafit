package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.groupclass.dto.GroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.GroupClassResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupClassResponse> create(@Valid @RequestBody GroupClassRequest req) {
        return ResponseEntity.ok(groupClassService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupClassResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody GroupClassRequest req
    ) {
        return ResponseEntity.ok(groupClassService.update(id, req));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        groupClassService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
