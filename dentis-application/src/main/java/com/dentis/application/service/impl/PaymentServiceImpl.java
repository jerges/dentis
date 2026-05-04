package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreatePaymentRequest;
import com.dentis.application.dto.response.PaymentResponse;
import com.dentis.application.mapper.PaymentMapper;
import com.dentis.application.service.PaymentService;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.domain.entity.Budget;
import com.dentis.domain.entity.Payment;
import com.dentis.domain.enums.BudgetStatus;
import com.dentis.domain.enums.PaymentStatus;
import com.dentis.domain.repository.BudgetRepository;
import com.dentis.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse register(CreatePaymentRequest request) {
        Budget budget = budgetRepository.findById(request.getBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", request.getBudgetId()));

        if (budget.getStatus() == BudgetStatus.REJECTED || budget.getStatus() == BudgetStatus.EXPIRED) {
            throw new BusinessException("Cannot register payment for a rejected or expired budget",
                    "INVALID_BUDGET_STATUS");
        }

        Payment payment = Payment.builder()
                .budget(budget)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .reference(request.getReference())
                .invoiceNumber(request.getInvoiceNumber())
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);
        updateBudgetPaymentStatus(budget);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> findByBudget(String budgetId) {
        return paymentRepository.findByBudgetId(budgetId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    private void updateBudgetPaymentStatus(Budget budget) {
        BigDecimal totalPaid = paymentRepository.sumAmountByBudgetId(budget.getId());
        BigDecimal totalAmount = budget.getTotalAmount();
        BigDecimal balance = totalAmount.subtract(totalPaid);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            budget.setPaymentStatus(PaymentStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            budget.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            budget.setPaymentStatus(PaymentStatus.PENDING);
        }
        budgetRepository.save(budget);
    }
}
