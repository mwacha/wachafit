package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.billing.PaymentCharge;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock MemberSubscriptionRepository subscriptionRepo;
    @Mock MembershipPlanRepository planRepo;
    @Mock PaymentChargeRepository chargeRepo;
    @Mock UserRepository userRepo;
    @InjectMocks MembershipService service;

    private UUID studentId;
    private User adminUser;
    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setRole(Role.ADMIN);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(adminUser, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        plan = new MembershipPlan();
        plan.setName("Plano Básico");
        plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("99.90"));
        try {
            var f = MembershipPlan.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(plan, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createSubscription_shouldPersistSubscriptionAndGenerateCharge() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(new User()));
        when(subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(false);
        when(planRepo.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> {
            MemberSubscription s = inv.getArgument(0);
            try {
                var f = MemberSubscription.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(s, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return s;
        });

        CreateSubscriptionRequest req = new CreateSubscriptionRequest(plan.getId(), LocalDate.of(2026, 7, 1));
        SubscriptionResponse res = service.createSubscription(studentId, req, adminUser.getId());

        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.expiresAt()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(res.planName()).isEqualTo("Plano Básico");
        verify(chargeRepo).save(any(PaymentCharge.class));
    }

    @Test
    void createSubscription_shouldThrowBusiness_whenAlreadyHasActiveSubscription() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(new User()));
        when(subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubscription(studentId,
            new CreateSubscriptionRequest(plan.getId(), LocalDate.now()),
            adminUser.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ativa");
    }

    @Test
    void createSubscription_shouldThrowBusiness_whenPlanInactive() {
        plan.setActive(false);
        when(userRepo.findById(studentId)).thenReturn(Optional.of(new User()));
        when(subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(false);
        when(planRepo.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.createSubscription(studentId,
            new CreateSubscriptionRequest(plan.getId(), LocalDate.now()),
            adminUser.getId()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("inativo");
    }

    @Test
    void cancelSubscription_shouldSetCancelledAndCancelPendingCharges() {
        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId);
        sub.setStatus("ACTIVE");
        try {
            var f = MemberSubscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        when(subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(Optional.of(sub));

        service.cancelSubscription(studentId, "Mudança de planos", adminUser);

        assertThat(sub.getStatus()).isEqualTo("CANCELLED");
        assertThat(sub.getCancelledAt()).isNotNull();
        assertThat(sub.getCancellationReason()).isEqualTo("Mudança de planos");
        verify(chargeRepo).cancelPendingBySubscriptionId(sub.getId());
    }

    @Test
    void getActiveSubscription_shouldThrowForbidden_whenStudentAccessesOther() {
        User otherStudent = new User();
        otherStudent.setRole(Role.STUDENT);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(otherStudent, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        assertThatThrownBy(() -> service.getActiveSubscription(studentId, otherStudent))
            .isInstanceOf(ForbiddenException.class);
    }
}
