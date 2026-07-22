package com.github.mwacha.wachafit.progress;

import com.github.mwacha.wachafit.progress.dto.PhotoResponse;
import com.github.mwacha.wachafit.user.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class ProgressController {

    private final ProgressService service;

    public ProgressController(ProgressService service) { this.service = service; }

    @PostMapping(value = "/api/students/{studentId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','TRAINER','PROFESSOR')")
    public ResponseEntity<PhotoResponse> upload(
            @PathVariable UUID studentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) LocalDate takenAt,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.upload(studentId, file, takenAt, notes, currentUser));
    }

    @GetMapping("/api/students/{studentId}/photos")
    @PreAuthorize("isAuthenticated()")
    public List<PhotoResponse> list(@PathVariable UUID studentId,
            @AuthenticationPrincipal User currentUser) {
        return service.list(studentId, currentUser);
    }

    @GetMapping("/api/photos/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getFile(@PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        Resource resource = service.loadFile(id, currentUser);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @DeleteMapping("/api/photos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
