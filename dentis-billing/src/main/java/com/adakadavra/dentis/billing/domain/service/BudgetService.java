package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final PaymentRepository paymentRepository;
    private final TariffRepository tariffRepository;

    @Transactional
    public Budget createBudget(Budget budget) {
        validateBudgetItems(budget);
        Budget draft = budget.withStatus(BudgetStatus.DRAFT).withCreatedAt(LocalDateTime.now());
        return budgetRepository.save(draft);
    }

    @Transactional
    public Budget approveBudget(UUID budgetId) {
        Budget budget = findBudgetOrThrow(budgetId);
        if (budget.getStatus() != BudgetStatus.PRESENTED && budget.getStatus() != BudgetStatus.DRAFT) {
            throw new BusinessRuleException("Budget cannot be approved in its current status", "INVALID_BUDGET_STATUS");
        }
        Budget approved = budget.withStatus(BudgetStatus.APPROVED).withApprovedAt(LocalDateTime.now());
        return budgetRepository.save(approved);
    }

    public BudgetSummary getBudgetSummary(UUID budgetId) {
        Budget budget = findBudgetOrThrow(budgetId);
        BigDecimal totalPaid = paymentRepository.sumPaymentsByBudgetId(budgetId);
        BigDecimal balance = budget.grandTotal().subtract(totalPaid);

        return BudgetSummary.builder()
                .budgetId(budgetId)
                .grandTotal(budget.grandTotal())
                .totalPaid(totalPaid)
                .balance(balance)
                .status(balance.compareTo(BigDecimal.ZERO) <= 0
                        ? PaymentSummaryStatus.PAID
                        : totalPaid.compareTo(BigDecimal.ZERO) > 0
                            ? PaymentSummaryStatus.PARTIALLY_PAID
                            : PaymentSummaryStatus.PENDING)
                .build();
    }

    public Budget findById(UUID id) {
        return findBudgetOrThrow(id);
    }

    private void validateBudgetItems(Budget budget) {
        if (budget.getItems() == null || budget.getItems().isEmpty()) {
            throw new BusinessRuleException("Budget must have at least one item", "EMPTY_BUDGET");
        }
        budget.getItems().forEach(item -> {
            if (!tariffRepository.findById(item.getTariffId()).isPresent()) {
                throw new ResourceNotFoundException("Tariff", item.getTariffId());
            }
        });
    }

    private Budget findBudgetOrThrow(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }
}
