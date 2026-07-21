package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.*;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class TrainerProfileService {

    private final TrainerProfileRepository repo;
    private final UserRepository userRepo;

    public TrainerProfileService(TrainerProfileRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public TrainerProfileResponse upsert(UUID trainerId, CreateTrainerProfileRequest req, User requestingUser) {
        userRepo.findById(trainerId).orElseThrow(() -> new NotFoundException("Trainer not found"));
        TrainerProfile p = repo.findByUserId(trainerId).orElse(new TrainerProfile());
        p.setUserId(trainerId);
        p.setCref(req.cref());
        p.setSpecialties(req.specialties());
        p.setBio(req.bio());
        p.setContractType(req.contractType());
        p.setAdmissionDate(req.admissionDate());
        p.setCommissionType(req.commissionType());
        p.setCommissionValue(req.commissionValue());
        return toResponse(repo.save(p));
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse get(UUID trainerId) {
        return repo.findByUserId(trainerId).map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Trainer profile not found"));
    }

    private TrainerProfileResponse toResponse(TrainerProfile p) {
        return new TrainerProfileResponse(p.getId(), p.getUserId(), p.getCref(),
            p.getSpecialties(), p.getBio(), p.getContractType(), p.getAdmissionDate(),
            p.getCommissionType(), p.getCommissionValue(), p.getCreatedAt());
    }
}
