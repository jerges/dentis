package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;

    @Transactional
    public Payment registerPayment(Payment payment) {
        Budget budget = budgetRepository.findById(payment.getBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", payment.getBudgetId()));

        if (budget.getStatus() != BudgetStatus.APPROVED) {
            throw new BusinessRuleException("Payments can only be registered for APPROVED budgets", "BUDGET_NOT_APPROVED");
        }

        validatePaymentAmount(payment, budget);

        Payment withTimestamp = Payment.builder()
                .patientId(payment.getPatientId())
                .budgetId(payment.getBudgetId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .invoiceReference(payment.getInvoiceReference())
                .notes(payment.getNotes())
                .paidAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(withTimestamp);
    }

    public List<Payment> findByBudgetId(UUID budgetId) {
        return paymentRepository.findByBudgetId(budgetId);
    }

    public List<Payment> findByPatientId(UUID patientId) {
        return paymentRepository.findByPatientId(patientId);
    }

    private void validatePaymentAmount(Payment payment, Budget budget) {
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero", "INVALID_PAYMENT_AMOUNT");
        }
        BigDecimal alreadyPaid = paymentRepository.sumPaymentsByBudgetId(payment.getBudgetId());
        BigDecimal remaining = budget.grandTotal().subtract(alreadyPaid);
        if (payment.getAmount().compareTo(remaining) > 0) {
            throw new BusinessRuleException(
                "Payment amount exceeds remaining balance of " + remaining,
                "PAYMENT_EXCEEDS_BALANCE"
            );
        }
    }
}
