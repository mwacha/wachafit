package com.github.mwacha.wachafit.membership;

import com.github.mwacha.wachafit.billing.PaymentCharge;
import com.github.mwacha.wachafit.billing.PaymentChargeRepository;
import com.github.mwacha.wachafit.membership.dto.CreateSubscriptionRequest;
import com.github.mwacha.wachafit.membership.dto.SubscriptionResponse;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.tenant.TenantContext;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class MembershipService {

    private final MemberSubscriptionRepository subscriptionRepo;
    private final MembershipPlanRepository planRepo;
    private final PaymentChargeRepository chargeRepo;
    private final UserRepository userRepo;

    public MembershipService(MemberSubscriptionRepository subscriptionRepo,
                             MembershipPlanRepository planRepo,
                             PaymentChargeRepository chargeRepo,
                             UserRepository userRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.chargeRepo = chargeRepo;
        this.userRepo = userRepo;
    }

    public SubscriptionResponse createSubscription(UUID studentId, CreateSubscriptionRequest req, UUID createdBy) {
        if (!userRepo.existsByIdAndTenantId(studentId, TenantContext.get())) {
            throw new NotFoundException("Aluno não encontrado");
        }

        if (subscriptionRepo.existsByStudentIdAndStatus(studentId, "ACTIVE")) {
            throw new BusinessException("Aluno já possui uma assinatura ativa");
        }

        MembershipPlan plan = planRepo.findById(req.planId())
            .orElseThrow(() -> new NotFoundException("Plano não encontrado"));

        if (!plan.isActive()) {
            throw new BusinessException("Plano inativo");
        }

        MemberSubscription sub = new MemberSubscription();
        sub.setStudentId(studentId);
        sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE");
        sub.setStartedAt(req.startedAt());
        sub.setExpiresAt(req.startedAt().plusMonths(plan.getDurationMonths()));
        sub.setCreatedBy(createdBy);
        MemberSubscription saved = subscriptionRepo.save(sub);

        PaymentCharge charge = new PaymentCharge();
        charge.setSubscriptionId(saved.getId());
        charge.setStudentId(studentId);
        charge.setAmount(plan.getPrice());
        charge.setDueDate(req.startedAt());
        charge.setStatus("PENDING");
        chargeRepo.save(charge);

        return toResponse(saved, plan.getName());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")
            .map(sub -> {
                String planName = planRepo.findById(sub.getPlanId())
                    .map(MembershipPlan::getName).orElse("Plano removido");
                return toResponse(sub, planName);
            }).orElse(null);
    }

    public void cancelSubscription(UUID studentId, String cancellationReason, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        MemberSubscription sub = subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")
            .orElseThrow(() -> new NotFoundException("Assinatura ativa não encontrada"));
        sub.setStatus("CANCELLED");
        sub.setCancelledAt(LocalDate.now());
        sub.setCancellationReason(cancellationReason);
        subscriptionRepo.save(sub);
        chargeRepo.cancelPendingBySubscriptionId(sub.getId());
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private SubscriptionResponse toResponse(MemberSubscription sub, String planName) {
        return new SubscriptionResponse(sub.getId(), sub.getStudentId(), sub.getPlanId(),
            planName, sub.getStatus(), sub.getStartedAt(), sub.getExpiresAt(), sub.getCreatedAt());
    }
}
