package com.github.mwacha.wachafit.profile;

import com.github.mwacha.wachafit.profile.dto.CreateTrainerProfileRequest;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerProfileServiceTest {

    @Mock TrainerProfileRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks TrainerProfileService service;

    @Test
    void upsert_throwsNotFound_whenTrainerBelongsToDifferentTenant() {
        UUID trainerId = UUID.randomUUID();
        UUID myTenantId = UUID.randomUUID();
        TenantContext.set(myTenantId);
        try {
            when(userRepo.existsByIdAndTenantId(trainerId, myTenantId)).thenReturn(false);

            assertThatThrownBy(() -> service.upsert(trainerId,
                new CreateTrainerProfileRequest("CREF123", "Musculação", "bio", "CLT",
                    null, "FIXED", null),
                null))
                .isInstanceOf(NotFoundException.class);

            verify(repo, never()).save(any());
        } finally {
            TenantContext.clear();
        }
    }
}
