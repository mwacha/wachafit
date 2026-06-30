package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.shared.exception.*;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository profileRepo;
    private final StudentHealthRepository healthRepo;
    private final UserRepository userRepo;

    public StudentProfileService(StudentProfileRepository profileRepo,
            StudentHealthRepository healthRepo, UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.healthRepo = healthRepo;
        this.userRepo = userRepo;
    }

    public StudentProfileResponse createProfile(UUID studentId, CreateStudentProfileRequest req, UUID createdById) {
        userRepo.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));
        if (profileRepo.findByUserId(studentId).isPresent())
            throw new BusinessException("Profile already exists for this student");
        if (profileRepo.existsByCpfAndUserIdNot(req.cpf(), studentId))
            throw new BusinessException("CPF already registered");
        StudentProfile p = new StudentProfile();
        p.setUserId(studentId);
        p.setCpf(req.cpf());
        p.setBirthDate(req.birthDate());
        p.setPhone(req.phone());
        p.setAddressLine(req.addressLine());
        p.setAddressCity(req.addressCity());
        p.setAddressState(req.addressState());
        p.setAddressZip(req.addressZip());
        p.setEmergencyContactName(req.emergencyContactName());
        p.setEmergencyContactPhone(req.emergencyContactPhone());
        return toResponse(profileRepo.save(p));
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return profileRepo.findByUserId(studentId).map(this::toResponse).orElse(null);
    }

    public StudentProfileResponse updateProfile(UUID studentId, CreateStudentProfileRequest req, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        StudentProfile p = profileRepo.findByUserId(studentId)
            .orElseThrow(() -> new NotFoundException("Profile not found"));
        if (!p.getCpf().equals(req.cpf()) && profileRepo.existsByCpfAndUserIdNot(req.cpf(), studentId))
            throw new BusinessException("CPF already registered");
        p.setCpf(req.cpf());
        p.setBirthDate(req.birthDate());
        p.setPhone(req.phone());
        p.setAddressLine(req.addressLine());
        p.setAddressCity(req.addressCity());
        p.setAddressState(req.addressState());
        p.setAddressZip(req.addressZip());
        p.setEmergencyContactName(req.emergencyContactName());
        p.setEmergencyContactPhone(req.emergencyContactPhone());
        return toResponse(profileRepo.save(p));
    }

    public StudentHealthResponse upsertHealth(UUID studentId, StudentHealthRequest req, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        StudentHealth h = healthRepo.findByUserId(studentId).orElse(new StudentHealth());
        h.setUserId(studentId);
        h.setHasHeartCondition(req.hasHeartCondition());
        h.setHasDiabetes(req.hasDiabetes());
        h.setHasHypertension(req.hasHypertension());
        h.setMedications(req.medications());
        h.setPhysicalRestrictions(req.physicalRestrictions());
        h.setParqSignedAt(req.parqSignedAt());
        h.setNotes(req.notes());
        StudentHealth saved = healthRepo.save(h);
        return new StudentHealthResponse(saved.getId(), saved.getUserId(), saved.isHasHeartCondition(),
            saved.isHasDiabetes(), saved.isHasHypertension(), saved.getMedications(),
            saved.getPhysicalRestrictions(), saved.getParqSignedAt(), saved.getNotes());
    }

    @Transactional(readOnly = true)
    public StudentHealthResponse getHealth(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return healthRepo.findByUserId(studentId).map(h ->
            new StudentHealthResponse(h.getId(), h.getUserId(), h.isHasHeartCondition(),
                h.isHasDiabetes(), h.isHasHypertension(), h.getMedications(),
                h.getPhysicalRestrictions(), h.getParqSignedAt(), h.getNotes()))
            .orElse(null);
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId()))
            throw new ForbiddenException("Access denied");
    }

    private StudentProfileResponse toResponse(StudentProfile p) {
        return new StudentProfileResponse(p.getId(), p.getUserId(), p.getCpf(), p.getBirthDate(),
            p.getPhone(), p.getAddressLine(), p.getAddressCity(), p.getAddressState(),
            p.getAddressZip(), p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.getCreatedAt());
    }
}
