package com.dentis.application.service;

import com.dentis.application.dto.request.CreatePaymentRequest;
import com.dentis.application.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse register(CreatePaymentRequest request);

    List<PaymentResponse> findByBudget(String budgetId);
}
