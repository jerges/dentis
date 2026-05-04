package com.dentis.application.service;

import com.dentis.application.dto.request.CreateBudgetRequest;
import com.dentis.application.dto.response.BudgetResponse;
import com.dentis.common.response.PageResponse;
import com.dentis.domain.enums.BudgetStatus;
import org.springframework.data.domain.Pageable;

public interface BudgetService {

    BudgetResponse create(CreateBudgetRequest request);

    BudgetResponse findById(String id);

    PageResponse<BudgetResponse> findByPatient(String patientId, Pageable pageable);

    BudgetResponse approve(String id);

    BudgetResponse reject(String id);

    void markItemPerformed(String budgetId, String itemId);
}
