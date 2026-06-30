package com.github.mwacha.wachafit.billing;

import com.github.mwacha.wachafit.billing.dto.ChargeResponse;
import com.github.mwacha.wachafit.billing.dto.CreateChargeRequest;
import com.github.mwacha.wachafit.billing.dto.ManualPaymentRequest;
import com.github.mwacha.wachafit.membership.MemberSubscriptionRepository;
import com.github.mwacha.wachafit.shared.exception.BusinessException;
import com.github.mwacha.wachafit.shared.exception.ForbiddenException;
import com.github.mwacha.wachafit.shared.exception.NotFoundException;
import com.github.mwacha.wachafit.user.Role;
import com.github.mwacha.wachafit.user.User;
import com.github.mwacha.wachafit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BillingService {

    private final PaymentChargeRepository chargeRepo;
    private final MemberSubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;

    public BillingService(PaymentChargeRepository chargeRepo,
                          MemberSubscriptionRepository subscriptionRepo,
                          UserRepository userRepo) {
        this.chargeRepo = chargeRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public List<ChargeResponse> listCharges(UUID studentId, User requestingUser) {
        assertCanAccess(studentId, requestingUser);
        return chargeRepo.findByStudentIdOrderByCreatedAtDesc(studentId)
            .stream().map(this::toResponse).toList();
    }

    public ChargeResponse createManualCharge(UUID studentId, CreateChargeRequest req, User requestingUser) {
        userRepo.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));
        var sub = subscriptionRepo.findByStudentIdAndStatus(studentId, "ACTIVE")
            .orElseThrow(() -> new BusinessException("Aluno não possui assinatura ativa"));
        PaymentCharge charge = new PaymentCharge();
        charge.setStudentId(studentId);
        charge.setSubscriptionId(sub.getId());
        charge.setAmount(req.amount());
        charge.setDueDate(req.dueDate());
        charge.setStatus("PENDING");
        return toResponse(chargeRepo.save(charge));
    }

    public ChargeResponse payCharge(UUID chargeId, ManualPaymentRequest req, User requestingUser) {
        PaymentCharge charge = chargeRepo.findById(chargeId)
            .orElseThrow(() -> new NotFoundException("Cobrança não encontrada"));
        if ("PAID".equals(charge.getStatus()) || "CANCELLED".equals(charge.getStatus())) {
            throw new BusinessException("Cobrança já " + charge.getStatus().toLowerCase());
        }
        charge.setStatus("PAID");
        charge.setPaidAt(OffsetDateTime.now());
        charge.setPaymentMethod(req.paymentMethod());
        charge.setGateway("MANUAL");
        return toResponse(chargeRepo.save(charge));
    }

    public void cancelCharge(UUID chargeId) {
        PaymentCharge charge = chargeRepo.findById(chargeId)
            .orElseThrow(() -> new NotFoundException("Cobrança não encontrada"));
        if ("PAID".equals(charge.getStatus())) {
            throw new BusinessException("Não é possível cancelar cobrança já paga");
        }
        if ("CANCELLED".equals(charge.getStatus())) {
            throw new BusinessException("Cobrança já cancelada");
        }
        charge.setStatus("CANCELLED");
        chargeRepo.save(charge);
    }

    public void processWebhookCharge(String externalChargeId, String newStatus) {
        chargeRepo.findByExternalChargeId(externalChargeId).ifPresent(charge -> {
            if ("PAID".equals(newStatus) && !"PAID".equals(charge.getStatus())) {
                charge.setStatus("PAID");
                charge.setPaidAt(OffsetDateTime.now());
                chargeRepo.save(charge);
            } else if ("CANCELLED".equals(newStatus) && !"PAID".equals(charge.getStatus())) {
                charge.setStatus("CANCELLED");
                chargeRepo.save(charge);
            }
        });
    }

    private void assertCanAccess(UUID studentId, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT && !studentId.equals(requestingUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private ChargeResponse toResponse(PaymentCharge c) {
        return new ChargeResponse(c.getId(), c.getStudentId(), c.getSubscriptionId(),
            c.getAmount(), c.getDueDate(), c.getStatus(), c.getPaidAt(),
            c.getPaymentMethod(), c.getExternalPaymentUrl(), c.getCreatedAt());
    }
}
