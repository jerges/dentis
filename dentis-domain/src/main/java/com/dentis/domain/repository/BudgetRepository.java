package com.dentis.domain.repository;

import com.dentis.domain.entity.Budget;
import com.dentis.domain.enums.BudgetStatus;
import com.dentis.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, String> {

    Page<Budget> findByPatientId(String patientId, Pageable pageable);

    List<Budget> findByPatientIdAndStatus(String patientId, BudgetStatus status);

    List<Budget> findByPatientIdAndPaymentStatus(String patientId, PaymentStatus paymentStatus);

    Page<Budget> findByDentistId(String dentistId, Pageable pageable);
}
