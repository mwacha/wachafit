package com.github.mwacha.wachafit.groupclass;

import com.github.mwacha.wachafit.booking.BookingService;
import com.github.mwacha.wachafit.groupclass.dto.CreateGroupClassRequest;
import com.github.mwacha.wachafit.groupclass.dto.EnrollStudentRequest;
import com.github.mwacha.wachafit.groupclass.dto.EnrolledClassResponse;
import com.github.mwacha.wachafit.groupclass.dto.EnrolledStudentResponse;
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
    private final BookingService bookingService;

    public GroupClassController(GroupClassService groupClassService, BookingService bookingService) {
        this.groupClassService = groupClassService;
        this.bookingService = bookingService;
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<List<EnrolledClassResponse>> myEnrollments(
        @AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser
    ) {
        return ResponseEntity.ok(groupClassService.getStudentEnrollments(currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<GroupClassResponse>> list(
        @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(groupClassService.list(active));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<GroupClassResponse> create(
        @Valid @RequestBody CreateGroupClassRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupClassService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<GroupClassResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateGroupClassRequest req,
        @AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser
    ) {
        return ResponseEntity.ok(
            groupClassService.updateGroupClass(id, req, currentUser.getId(), currentUser.getRole()));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<GroupClassResponse> reactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(groupClassService.reactivateGroupClass(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<Void> deactivate(
        @PathVariable UUID id,
        @AuthenticationPrincipal com.github.mwacha.wachafit.user.User currentUser
    ) {
        groupClassService.deactivateGroupClass(id, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{classId}/enrolled")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<List<EnrolledStudentResponse>> listEnrolled(@PathVariable UUID classId) {
        return ResponseEntity.ok(bookingService.listEnrolledStudents(classId));
    }

    @PostMapping("/{classId}/enrolled")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<Void> enrollStudent(
        @PathVariable UUID classId,
        @RequestBody EnrollStudentRequest req
    ) {
        bookingService.enrollStudentInClass(classId, UUID.fromString(req.studentId()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{classId}/enrolled/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER','PROFESSOR')")
    public ResponseEntity<Void> unenrollStudent(
        @PathVariable UUID classId,
        @PathVariable UUID studentId
    ) {
        bookingService.unenrollStudentFromClass(classId, studentId);
        return ResponseEntity.noContent().build();
    }
}
