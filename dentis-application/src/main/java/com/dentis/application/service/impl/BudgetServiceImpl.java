package com.dentis.application.service.impl;

import com.dentis.application.dto.request.CreateBudgetRequest;
import com.dentis.application.dto.response.BudgetResponse;
import com.dentis.application.mapper.BudgetMapper;
import com.dentis.application.service.BudgetService;
import com.dentis.common.constant.AppConstants;
import com.dentis.common.exception.BusinessException;
import com.dentis.common.exception.ResourceNotFoundException;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.entity.Budget;
import com.dentis.domain.entity.BudgetItem;
import com.dentis.domain.entity.FeeItem;
import com.dentis.domain.enums.BudgetStatus;
import com.dentis.domain.enums.TreatmentStatus;
import com.dentis.domain.repository.BudgetRepository;
import com.dentis.domain.repository.DentistRepository;
import com.dentis.domain.repository.FeeItemRepository;
import com.dentis.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final FeeItemRepository feeItemRepository;
    private final BudgetMapper budgetMapper;

    @Override
    @Transactional
    public BudgetResponse create(CreateBudgetRequest request) {
        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientId()));

        Budget budget = Budget.builder()
                .patient(patient)
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .notes(request.getNotes())
                .currency(request.getCurrency())
                .status(BudgetStatus.DRAFT)
                .build();

        if (request.getDentistId() != null) {
            budget.setDentist(dentistRepository.findById(request.getDentistId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dentist", request.getDentistId())));
        }

        List<BudgetItem> items = request.getItems().stream().map(itemReq -> {
            BudgetItem item = BudgetItem.builder()
                    .budget(budget)
                    .description(itemReq.getDescription())
                    .unitPrice(itemReq.getUnitPrice())
                    .discountPercentage(itemReq.getDiscountPercentage() != null
                            ? itemReq.getDiscountPercentage() : BigDecimal.ZERO)
                    .quantity(itemReq.getQuantity())
                    .toothNumber(itemReq.getToothNumber())
                    .procedureStatus(TreatmentStatus.PENDING)
                    .build();

            if (itemReq.getFeeItemId() != null) {
                FeeItem feeItem = feeItemRepository.findById(itemReq.getFeeItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("FeeItem", itemReq.getFeeItemId()));
                item.setFeeItem(feeItem);
            }
            return item;
        }).toList();

        budget.setItems(items);
        return budgetMapper.toResponse(budgetRepository.save(budget));
    }

    @Override
    public BudgetResponse findById(String id) {
        return budgetMapper.toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<BudgetResponse> findByPatient(String patientId, Pageable pageable) {
        Page<Budget> page = budgetRepository.findByPatientId(patientId, pageable);
        return PageResponse.of(
                page.getContent().stream().map(budgetMapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements()
        );
    }

    @Override
    @Transactional
    public BudgetResponse approve(String id) {
        Budget budget = findOrThrow(id);
        if (budget.getStatus() != BudgetStatus.DRAFT && budget.getStatus() != BudgetStatus.SENT) {
            throw new BusinessException("Only DRAFT or SENT budgets can be approved", "INVALID_BUDGET_STATUS");
        }
        budget.setStatus(BudgetStatus.APPROVED);
        return budgetMapper.toResponse(budgetRepository.save(budget));
    }

    @Override
    @Transactional
    public BudgetResponse reject(String id) {
        Budget budget = findOrThrow(id);
        budget.setStatus(BudgetStatus.REJECTED);
        return budgetMapper.toResponse(budgetRepository.save(budget));
    }

    @Override
    @Transactional
    public void markItemPerformed(String budgetId, String itemId) {
        Budget budget = findOrThrow(budgetId);
        BudgetItem item = budget.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("BudgetItem", itemId));
        item.setPerformedDate(LocalDate.now());
        item.setProcedureStatus(TreatmentStatus.COMPLETED);
        budgetRepository.save(budget);
    }

    private Budget findOrThrow(String id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }
}
