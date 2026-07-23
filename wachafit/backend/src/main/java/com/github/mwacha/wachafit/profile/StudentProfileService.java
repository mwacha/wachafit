package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.shared.exception.*;
import com.github.mwacha.wachafit.tenant.TenantContext;
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
        if (!userRepo.existsByIdAndTenantId(studentId, TenantContext.get())) {
            throw new NotFoundException("Student not found");
        }
        if (profileRepo.findByUserId(studentId).isPresent())
            throw new BusinessException("Profile already exists for this student");
        if (profileRepo.existsByCpfAndUserIdNot(req.cpf(), studentId))
            throw new BusinessException("CPF already registered");
        return toResponse(profileRepo.save(applyProfile(new StudentProfile(), studentId, req)));
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
        return toResponse(profileRepo.save(applyProfile(p, studentId, req)));
    }

    public StudentHealthResponse upsertHealth(UUID studentId, StudentHealthRequest req, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        StudentHealth h = healthRepo.findByUserId(studentId).orElse(new StudentHealth());
        h.setUserId(studentId);
        h.setHasHeartCondition(req.hasHeartCondition());
        h.setHasDiabetes(req.hasDiabetes());
        h.setHasHypertension(req.hasHypertension());
        h.setHasRespiratoryCondition(req.hasRespiratoryCondition());
        h.setHasOrthopedicCondition(req.hasOrthopedicCondition());
        h.setHadSurgery(req.hadSurgery());
        h.setSurgeryDescription(req.surgeryDescription());
        h.setHasChronicPain(req.hasChronicPain());
        h.setChronicPainLocation(req.chronicPainLocation());
        h.setMedications(req.medications());
        h.setPhysicalRestrictions(req.physicalRestrictions());
        h.setSmokes(req.smokes());
        h.setDrinksAlcohol(req.drinksAlcohol());
        h.setAlcoholFrequency(req.alcoholFrequency());
        h.setSleepHours(req.sleepHours());
        h.setStressLevel(req.stressLevel());
        h.setActivityLevel(req.activityLevel());
        h.setFitnessGoal(req.fitnessGoal());
        h.setFitnessLevel(req.fitnessLevel());
        h.setExerciseHistory(req.exerciseHistory());
        h.setParqHeartProblem(req.parqHeartProblem());
        h.setParqChestPainExercise(req.parqChestPainExercise());
        h.setParqChestPainRest(req.parqChestPainRest());
        h.setParqDizziness(req.parqDizziness());
        h.setParqBoneJoint(req.parqBoneJoint());
        h.setParqBloodPressureMeds(req.parqBloodPressureMeds());
        h.setParqOtherReason(req.parqOtherReason());
        h.setParqOtherReasonDetail(req.parqOtherReasonDetail());
        h.setParqSignedAt(req.parqSignedAt());
        h.setNotes(req.notes());
        return toHealthResponse(healthRepo.save(h));
    }

    @Transactional(readOnly = true)
    public StudentHealthResponse getHealth(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return healthRepo.findByUserId(studentId).map(this::toHealthResponse).orElse(null);
    }

    private StudentProfile applyProfile(StudentProfile p, UUID studentId, CreateStudentProfileRequest req) {
        p.setUserId(studentId);
        p.setCpf(req.cpf());
        p.setRg(req.rg());
        p.setBirthDate(req.birthDate());
        p.setGender(req.gender());
        p.setMaritalStatus(req.maritalStatus());
        p.setProfession(req.profession());
        p.setPhone(req.phone());
        p.setAddressZip(req.addressZip());
        p.setAddressLine(req.addressLine());
        p.setAddressNumber(req.addressNumber());
        p.setAddressComplement(req.addressComplement());
        p.setAddressNeighborhood(req.addressNeighborhood());
        p.setAddressCity(req.addressCity());
        p.setAddressState(req.addressState());
        p.setEmergencyContactName(req.emergencyContactName());
        p.setEmergencyContactPhone(req.emergencyContactPhone());
        p.setEmergencyContactRelationship(req.emergencyContactRelationship());
        return p;
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId()))
            throw new ForbiddenException("Access denied");
    }

    private StudentProfileResponse toResponse(StudentProfile p) {
        return new StudentProfileResponse(
            p.getId(), p.getUserId(),
            p.getCpf(), p.getRg(),
            p.getBirthDate(), p.getGender(), p.getMaritalStatus(), p.getProfession(),
            p.getPhone(),
            p.getAddressZip(), p.getAddressLine(), p.getAddressNumber(),
            p.getAddressComplement(), p.getAddressNeighborhood(),
            p.getAddressCity(), p.getAddressState(),
            p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.getEmergencyContactRelationship(),
            p.getCreatedAt()
        );
    }

    private StudentHealthResponse toHealthResponse(StudentHealth h) {
        return new StudentHealthResponse(
            h.getId(), h.getUserId(),
            h.isHasHeartCondition(), h.isHasDiabetes(), h.isHasHypertension(),
            h.isHasRespiratoryCondition(), h.isHasOrthopedicCondition(),
            h.isHadSurgery(), h.getSurgeryDescription(),
            h.isHasChronicPain(), h.getChronicPainLocation(),
            h.getMedications(), h.getPhysicalRestrictions(),
            h.isSmokes(), h.isDrinksAlcohol(), h.getAlcoholFrequency(),
            h.getSleepHours(), h.getStressLevel(), h.getActivityLevel(),
            h.getFitnessGoal(), h.getFitnessLevel(), h.getExerciseHistory(),
            h.isParqHeartProblem(), h.isParqChestPainExercise(), h.isParqChestPainRest(),
            h.isParqDizziness(), h.isParqBoneJoint(), h.isParqBloodPressureMeds(),
            h.isParqOtherReason(), h.getParqOtherReasonDetail(),
            h.getParqSignedAt(), h.getNotes()
        );
    }
}
