package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.Payment;
import com.adakadavra.dentis.billing.domain.model.PaymentMethod;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.PaymentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentEntityMapper")
class PaymentEntityMapperTest {

    private PaymentEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should return null when payment is null")
        void shouldReturnNullWhenPaymentIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from domain to entity")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            LocalDateTime paidAt = LocalDateTime.now();

            Payment payment = Payment.builder()
                    .id(id)
                    .patientId(patientId)
                    .budgetId(budgetId)
                    .amount(new BigDecimal("150.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .invoiceReference("INV-2026-001")
                    .notes("Pago parcial")
                    .paidAt(paidAt)
                    .build();

            PaymentEntity entity = mapper.toEntity(payment);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getPatientId()).isEqualTo(patientId);
            assertThat(entity.getBudgetId()).isEqualTo(budgetId);
            assertThat(entity.getAmount()).isEqualByComparingTo("150.00");
            assertThat(entity.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(entity.getInvoiceReference()).isEqualTo("INV-2026-001");
            assertThat(entity.getNotes()).isEqualTo("Pago parcial");
            assertThat(entity.getPaidAt()).isEqualTo(paidAt);
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should return null when entity is null")
        void shouldReturnNullWhenEntityIsNull() {
            assertThat(mapper.toDomain(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields from entity to domain")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            LocalDateTime paidAt = LocalDateTime.now();

            PaymentEntity entity = PaymentEntity.builder()
                    .id(id)
                    .patientId(patientId)
                    .budgetId(budgetId)
                    .amount(new BigDecimal("200.00"))
                    .paymentMethod(PaymentMethod.CASH)
                    .invoiceReference("INV-2026-002")
                    .notes("Pago completo")
                    .paidAt(paidAt)
                    .build();

            Payment domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getPatientId()).isEqualTo(patientId);
            assertThat(domain.getBudgetId()).isEqualTo(budgetId);
            assertThat(domain.getAmount()).isEqualByComparingTo("200.00");
            assertThat(domain.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(domain.getInvoiceReference()).isEqualTo("INV-2026-002");
            assertThat(domain.getNotes()).isEqualTo("Pago completo");
            assertThat(domain.getPaidAt()).isEqualTo(paidAt);
        }
    }
}
