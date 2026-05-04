package com.dentis.domain.repository;

import com.dentis.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByBudgetId(String budgetId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.budget.id = :budgetId")
    BigDecimal sumAmountByBudgetId(@Param("budgetId") String budgetId);
}
