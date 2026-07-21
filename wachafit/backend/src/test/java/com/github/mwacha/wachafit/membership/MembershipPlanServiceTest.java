package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.membership.dto.CreatePlanRequest;
import com.github.mwacha.wachafit.membership.dto.PlanResponse;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipPlanServiceTest {

    @Mock MembershipPlanRepository planRepo;
    @InjectMocks MembershipPlanService service;

    @Test
    void createPlan_shouldPersistAndReturnResponse() {
        CreatePlanRequest req = new CreatePlanRequest("Plano Básico", "Descrição", 1, new BigDecimal("99.90"), 3);
        when(planRepo.save(any())).thenAnswer(inv -> {
            MembershipPlan p = inv.getArgument(0);
            try {
                var f = MembershipPlan.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(p, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return p;
        });
        PlanResponse res = service.createPlan(req);
        assertThat(res.name()).isEqualTo("Plano Básico");
        assertThat(res.price()).isEqualByComparingTo("99.90");
        assertThat(res.durationMonths()).isEqualTo(1);
        assertThat(res.active()).isTrue();
    }

    @Test
    void deactivatePlan_shouldSetActiveFalse() {
        UUID id = UUID.randomUUID();
        MembershipPlan plan = new MembershipPlan();
        plan.setName("Plano Premium");
        plan.setDurationMonths(3);
        plan.setPrice(BigDecimal.valueOf(199));
        when(planRepo.findById(id)).thenReturn(Optional.of(plan));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.deactivatePlan(id);
        assertThat(plan.isActive()).isFalse();
    }

    @Test
    void updatePlan_shouldThrowNotFound_whenPlanAbsent() {
        UUID id = UUID.randomUUID();
        when(planRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updatePlan(id,
            new CreatePlanRequest("X", null, 1, BigDecimal.ONE, null)))
            .isInstanceOf(NotFoundException.class);
    }
}
