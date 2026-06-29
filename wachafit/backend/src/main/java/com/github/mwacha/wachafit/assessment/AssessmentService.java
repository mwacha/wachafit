package com.github.mwacha.wachafit.assessment;

import com.github.mwacha.wachafit.assessment.dto.*;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AssessmentService {

    private final PhysicalAssessmentRepository repo;
    private final UserRepository userRepo;

    public AssessmentService(PhysicalAssessmentRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public AssessmentResponse create(UUID studentId, CreateAssessmentRequest req, UUID assessedById) {
        userRepo.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        PhysicalAssessment a = new PhysicalAssessment();
        a.setStudentId(studentId);
        a.setAssessedBy(assessedById);
        a.setAssessedAt(req.assessedAt());
        a.setWeightKg(req.weightKg());
        a.setHeightCm(req.heightCm());
        a.setBodyFatPct(req.bodyFatPct());
        a.setBmi(req.bmi());
        a.setNotes(req.notes());
        if (req.measurements() != null) {
            for (MeasurementRequest m : req.measurements()) {
                AssessmentMeasurement measurement = new AssessmentMeasurement();
                measurement.setBodyPart(m.bodyPart());
                measurement.setValueCm(m.valueCm());
                measurement.setAssessment(a);
                a.getMeasurements().add(measurement);
            }
        }
        return toResponse(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findByStudentIdOrderByAssessedAtAsc(studentId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EvolutionPoint> evolution(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return repo.findEvolutionByStudentId(studentId);
    }

    public AssessmentResponse update(UUID id, CreateAssessmentRequest req, User requestingUser) {
        PhysicalAssessment a = repo.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found"));
        assertCanAccess(a.getStudentId(), requestingUser);
        a.setAssessedAt(req.assessedAt());
        a.setWeightKg(req.weightKg());
        a.setHeightCm(req.heightCm());
        a.setBodyFatPct(req.bodyFatPct());
        a.setBmi(req.bmi());
        a.setNotes(req.notes());
        a.getMeasurements().clear();
        if (req.measurements() != null) {
            for (MeasurementRequest m : req.measurements()) {
                AssessmentMeasurement measurement = new AssessmentMeasurement();
                measurement.setBodyPart(m.bodyPart());
                measurement.setValueCm(m.valueCm());
                measurement.setAssessment(a);
                a.getMeasurements().add(measurement);
            }
        }
        return toResponse(repo.save(a));
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private AssessmentResponse toResponse(PhysicalAssessment a) {
        List<MeasurementResponse> measurements = a.getMeasurements().stream()
            .map(m -> new MeasurementResponse(m.getBodyPart(), m.getValueCm()))
            .toList();
        return new AssessmentResponse(
            a.getId(), a.getStudentId(), a.getAssessedBy(), a.getAssessedAt(),
            a.getWeightKg(), a.getHeightCm(), a.getBodyFatPct(), a.getBmi(),
            a.getNotes(), measurements, a.getCreatedAt()
        );
    }
}
