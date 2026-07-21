package com.github.mwacha.wachafit.billing;

import com.github.mwacha.wachafit.membership.MemberSubscription;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueJobServiceTest {

    @Mock PaymentChargeRepository chargeRepo;
    @Mock MemberSubscriptionRepository subscriptionRepo;
    @Mock PaymentProperties paymentProperties;
    @InjectMocks OverdueJobService service;

    @BeforeEach
    void setUp() {
        when(paymentProperties.getSuspendAfterDays()).thenReturn(5);
    }

    @Test
    void markOverdueAndSuspend_shouldMarkOverdueCharges() {
        when(chargeRepo.findSubscriptionIdsWithOverdueChargesOlderThan(any())).thenReturn(List.of());
        service.markOverdueAndSuspend();
        verify(chargeRepo).markOverdue(any(LocalDate.class));
    }

    @Test
    void markOverdueAndSuspend_shouldSuspendActiveSubscription_whenOverdueTooLong() {
        UUID subId = UUID.randomUUID();
        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(UUID.randomUUID());
        sub.setStatus("ACTIVE");
        try {
            var f = MemberSubscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, subId);
        } catch (Exception e) { throw new RuntimeException(e); }

        when(chargeRepo.findSubscriptionIdsWithOverdueChargesOlderThan(any())).thenReturn(List.of(subId));
        when(subscriptionRepo.findById(subId)).thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markOverdueAndSuspend();

        assertThat(sub.getStatus()).isEqualTo("SUSPENDED");
        verify(subscriptionRepo).save(sub);
    }

    @Test
    void markOverdueAndSuspend_shouldNotSuspend_whenSubscriptionAlreadySuspended() {
        UUID subId = UUID.randomUUID();
        MemberSubscription sub = new MemberSubscription();
        sub.setStatus("SUSPENDED");
        try {
            var f = MemberSubscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, subId);
        } catch (Exception e) { throw new RuntimeException(e); }

        when(chargeRepo.findSubscriptionIdsWithOverdueChargesOlderThan(any())).thenReturn(List.of(subId));
        when(subscriptionRepo.findById(subId)).thenReturn(Optional.of(sub));

        service.markOverdueAndSuspend();

        assertThat(sub.getStatus()).isEqualTo("SUSPENDED");
        verify(subscriptionRepo, never()).save(any());
    }
}
