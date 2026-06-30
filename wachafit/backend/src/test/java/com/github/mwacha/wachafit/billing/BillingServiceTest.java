package com.github.mwacha.wachafit.billing;

import com.github.mwacha.wachafit.billing.dto.ChargeResponse;
import com.github.mwacha.wachafit.billing.dto.CreateChargeRequest;
import com.github.mwacha.wachafit.billing.dto.ManualPaymentRequest;
import com.github.mwacha.wachafit.membership.MemberSubscription;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
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
class BillingServiceTest {

    @Mock PaymentChargeRepository chargeRepo;
    @Mock MemberSubscriptionRepository subscriptionRepo;
    @Mock UserRepository userRepo;
    @InjectMocks BillingService service;

    private UUID studentId;
    private User adminUser;
    private User studentUser;
    private PaymentCharge pendingCharge;
    private UUID chargeId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        chargeId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setRole(Role.ADMIN);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(adminUser, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        studentUser = new User();
        studentUser.setRole(Role.STUDENT);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(studentUser, studentId);
        } catch (Exception e) { throw new RuntimeException(e); }

        pendingCharge = new PaymentCharge();
        pendingCharge.setStudentId(studentId);
        pendingCharge.setSubscriptionId(UUID.randomUUID());
        pendingCharge.setAmount(new BigDecimal("99.90"));
        pendingCharge.setDueDate(LocalDate.now());
        pendingCharge.setStatus("PENDING");
        try {
            var f = PaymentCharge.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(pendingCharge, chargeId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void listCharges_shouldReturnChargesForStudent() {
        when(chargeRepo.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of(pendingCharge));
        List<ChargeResponse> result = service.listCharges(studentId, adminUser);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void listCharges_shouldThrowForbidden_whenStudentAccessesOther() {
        UUID otherId = UUID.randomUUID();
        assertThatThrownBy(() -> service.listCharges(otherId, studentUser))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void payCharge_shouldSetPaidAndMethod() {
        when(chargeRepo.findById(chargeId)).thenReturn(Optional.of(pendingCharge));
        when(chargeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ChargeResponse res = service.payCharge(chargeId, new ManualPaymentRequest("CASH"));
        assertThat(res.status()).isEqualTo("PAID");
        assertThat(res.paymentMethod()).isEqualTo("CASH");
        assertThat(res.paidAt()).isNotNull();
    }

    @Test
    void payCharge_shouldThrowBusiness_whenAlreadyPaid() {
        pendingCharge.setStatus("PAID");
        when(chargeRepo.findById(chargeId)).thenReturn(Optional.of(pendingCharge));
        assertThatThrownBy(() -> service.payCharge(chargeId, new ManualPaymentRequest("CASH")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelCharge_shouldSetCancelled() {
        when(chargeRepo.findById(chargeId)).thenReturn(Optional.of(pendingCharge));
        when(chargeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.cancelCharge(chargeId);
        assertThat(pendingCharge.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelCharge_shouldThrowBusiness_whenAlreadyPaid() {
        pendingCharge.setStatus("PAID");
        when(chargeRepo.findById(chargeId)).thenReturn(Optional.of(pendingCharge));
        assertThatThrownBy(() -> service.cancelCharge(chargeId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void createManualCharge_shouldPersistCharge_whenStudentHasActiveSubscription() {
        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId);
        sub.setStatus("ACTIVE");
        try {
            var f = MemberSubscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        when(userRepo.findById(studentId)).thenReturn(Optional.of(studentUser));
        when(subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(Optional.of(sub));
        when(chargeRepo.save(any())).thenAnswer(inv -> {
            PaymentCharge c = inv.getArgument(0);
            try {
                var f = PaymentCharge.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(c, UUID.randomUUID());
            } catch (Exception e) { throw new RuntimeException(e); }
            return c;
        });

        ChargeResponse res = service.createManualCharge(studentId,
            new CreateChargeRequest(new BigDecimal("150.00"), LocalDate.of(2026, 8, 1)),
            adminUser);

        assertThat(res.amount()).isEqualByComparingTo("150.00");
        assertThat(res.status()).isEqualTo("PENDING");
    }

    @Test
    void createManualCharge_shouldThrowBusiness_whenNoActiveSubscription() {
        when(userRepo.findById(studentId)).thenReturn(Optional.of(studentUser));
        when(subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createManualCharge(studentId,
            new CreateChargeRequest(BigDecimal.TEN, LocalDate.now()), adminUser))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("assinatura ativa");
    }
}
