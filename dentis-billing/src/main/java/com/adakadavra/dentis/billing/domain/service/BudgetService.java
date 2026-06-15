package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.event.BudgetPresentedEvent;
import com.adakadavra.dentis.billing.domain.model.Budget;
import com.adakadavra.dentis.billing.domain.model.BudgetItem;
import com.adakadavra.dentis.billing.domain.model.BudgetStatus;
import com.adakadavra.dentis.billing.domain.model.BudgetSummary;
import com.adakadavra.dentis.billing.domain.model.PaymentSummaryStatus;
import com.adakadavra.dentis.billing.domain.repository.BudgetRepository;
import com.adakadavra.dentis.billing.domain.repository.PaymentRepository;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final PaymentRepository paymentRepository;
    private final TariffRepository tariffRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Budget createBudget(Budget budget) {
        validateBudgetItems(budget);
        Budget draft = budget.withStatus(BudgetStatus.DRAFT).withCreatedAt(LocalDateTime.now());
        Budget saved = budgetRepository.save(draft);
        eventPublisher.publishEvent(new BudgetPresentedEvent(this, saved));
        return saved;
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

    @Transactional
    public Budget markItemAsPerformed(UUID budgetId, UUID itemId) {
        Budget budget = findBudgetOrThrow(budgetId);
        boolean found = budget.getItems().stream().anyMatch(i -> itemId.equals(i.getId()));
        if (!found) {
            throw new ResourceNotFoundException("BudgetItem", itemId);
        }
        List<BudgetItem> updatedItems = budget.getItems().stream()
                .map(item -> itemId.equals(item.getId())
                        ? item.withPerformed(true).withPerformedAt(LocalDateTime.now())
                        : item)
                .toList();
        return budgetRepository.save(budget.withItems(updatedItems));
    }

    public Page<Budget> findByPatientId(UUID patientId, Pageable pageable) {
        return budgetRepository.findByPatientId(patientId, pageable);
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
