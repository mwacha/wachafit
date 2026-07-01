package com.github.mwacha.wachafit.pdf;

import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/{studentId}/pdf")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/evolution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> evolutionPdf(@PathVariable UUID studentId,
                                               @AuthenticationPrincipal User currentUser) {
        assertCanAccess(studentId, currentUser);
        byte[] pdf = pdfService.generateEvolutionPdf(studentId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evolucao.pdf\"")
            .body(pdf);
    }

    @GetMapping("/workout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> workoutPdf(@PathVariable UUID studentId,
                                             @AuthenticationPrincipal User currentUser) {
        assertCanAccess(studentId, currentUser);
        byte[] pdf = pdfService.generateWorkoutPdf(studentId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ficha-treino.pdf\"")
            .body(pdf);
    }

    private void assertCanAccess(UUID studentId, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT && !studentId.equals(currentUser.getId())) {
            throw new ForbiddenException("Acesso negado");
        }
    }
}
