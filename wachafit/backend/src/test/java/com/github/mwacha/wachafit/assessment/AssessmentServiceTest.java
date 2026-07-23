package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock PhysicalAssessmentRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks AssessmentService service;

    private User trainer;
    private User student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        student = new User();
        student.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(student, studentId); }
        catch (Exception e) { throw new RuntimeException(e); }

        trainer = new User();
        trainer.setRole(Role.TRAINER);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(trainer, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void create_shouldPersistAssessmentWithMeasurements() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(true);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAssessmentRequest req = new CreateAssessmentRequest(
            LocalDate.now(), new BigDecimal("75.5"), new BigDecimal("175.0"),
            new BigDecimal("18.5"), new BigDecimal("24.7"), "notes",
            List.of(new MeasurementRequest("waist", new BigDecimal("80.0")))
        );

        AssessmentResponse res = service.create(studentId, req, trainer.getId());
        assertThat(res.studentId()).isEqualTo(studentId);
        assertThat(res.measurements()).hasSize(1);
        assertThat(res.measurements().get(0).bodyPart()).isEqualTo("waist");
    }

    @Test
    void create_shouldThrowNotFound_whenStudentMissing() {
        when(userRepo.existsByIdAndTenantId(eq(studentId), any())).thenReturn(false);
        assertThatThrownBy(() -> service.create(studentId,
            new CreateAssessmentRequest(LocalDate.now(), null, null, null, null, null, List.of()),
            trainer.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_throwsNotFound_whenStudentBelongsToDifferentTenant() {
        UUID myTenantId = UUID.randomUUID();
        TenantContext.set(myTenantId);
        try {
            when(userRepo.existsByIdAndTenantId(studentId, myTenantId)).thenReturn(false);

            assertThatThrownBy(() -> service.create(studentId,
                new CreateAssessmentRequest(LocalDate.now(), null, null, null, null, null, List.of()),
                trainer.getId()))
                .isInstanceOf(NotFoundException.class);

            verify(repo, never()).save(any());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void list_shouldThrowForbidden_whenStudentAccessesOtherStudentData() {
        User otherStudent = new User();
        otherStudent.setRole(Role.STUDENT);
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(otherStudent, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }

        assertThatThrownBy(() -> service.list(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_shouldSucceed_whenStudentAccessesOwnData() {
        when(repo.findByStudentIdOrderByAssessedAtAsc(studentId)).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.list(studentId, student));
    }

    @Test
    void list_shouldSucceed_whenTrainerAccessesStudentData() {
        when(repo.findByStudentIdOrderByAssessedAtAsc(studentId)).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.list(studentId, trainer));
    }
}
