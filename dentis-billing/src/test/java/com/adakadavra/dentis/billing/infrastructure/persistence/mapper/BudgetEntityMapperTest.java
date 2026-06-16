package com.adakadavra.dentis.billing.infrastructure.persistence.mapper;

import com.adakadavra.dentis.billing.domain.model.*;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.BudgetEntity;
import com.adakadavra.dentis.billing.infrastructure.persistence.entity.BudgetItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BudgetEntityMapper")
class BudgetEntityMapperTest {

    private BudgetEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BudgetEntityMapper();
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should return null when budget is null")
        void shouldReturnNullWhenBudgetIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map all scalar fields")
        void shouldMapAllScalarFields() {
            UUID id = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            UUID treatmentPlanId = UUID.randomUUID();
            UUID dentistId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime approvedAt = LocalDateTime.now();

            Budget budget = Budget.builder()
                    .id(id)
                    .patientId(patientId)
                    .treatmentPlanId(treatmentPlanId)
                    .dentistId(dentistId)
                    .status(BudgetStatus.PRESENTED)
                    .notes("some notes")
                    .createdAt(createdAt)
                    .approvedAt(approvedAt)
                    .items(List.of())
                    .build();

            BudgetEntity entity = mapper.toEntity(budget);

            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getPatientId()).isEqualTo(patientId);
            assertThat(entity.getTreatmentPlanId()).isEqualTo(treatmentPlanId);
            assertThat(entity.getDentistId()).isEqualTo(dentistId);
            assertThat(entity.getStatus()).isEqualTo(BudgetStatus.PRESENTED);
            assertThat(entity.getNotes()).isEqualTo("some notes");
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getApprovedAt()).isEqualTo(approvedAt);
        }

        @Test
        @DisplayName("should map budget items with all fields")
        void shouldMapBudgetItemsWithAllFields() {
            UUID tariffId = UUID.randomUUID();
            LocalDateTime performedAt = LocalDateTime.now();

            BudgetItem item = BudgetItem.builder()
                    .id(UUID.randomUUID())
                    .tariffId(tariffId)
                    .description("Limpieza dental")
                    .quantity(2)
                    .unitPrice(new BigDecimal("50.00"))
                    .discountPercentage(new BigDecimal("10.00"))
                    .performed(true)
                    .performedAt(performedAt)
                    .paymentStatus(ProcedurePaymentStatus.PAID)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .items(List.of(item))
                    .build();

            BudgetEntity entity = mapper.toEntity(budget);

            assertThat(entity.getItems()).hasSize(1);
            BudgetItemEntity itemEntity = entity.getItems().get(0);
            assertThat(itemEntity.getTariffId()).isEqualTo(tariffId);
            assertThat(itemEntity.getDescription()).isEqualTo("Limpieza dental");
            assertThat(itemEntity.getQuantity()).isEqualTo(2);
            assertThat(itemEntity.getUnitPrice()).isEqualByComparingTo("50.00");
            assertThat(itemEntity.getDiscountPercentage()).isEqualByComparingTo("10.00");
            assertThat(itemEntity.isPerformed()).isTrue();
            assertThat(itemEntity.getPerformedAt()).isEqualTo(performedAt);
            assertThat(itemEntity.getPaymentStatus()).isEqualTo(ProcedurePaymentStatus.PAID);
        }

        @Test
        @DisplayName("should set item's budget reference to the parent entity")
        void shouldSetItemBudgetReference() {
            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .items(List.of(BudgetItem.builder()
                            .id(UUID.randomUUID())
                            .tariffId(UUID.randomUUID())
                            .description("item")
                            .quantity(1)
                            .unitPrice(BigDecimal.TEN)
                            .discountPercentage(BigDecimal.ZERO)
                            .paymentStatus(ProcedurePaymentStatus.PENDING)
                            .build()))
                    .build();

            BudgetEntity entity = mapper.toEntity(budget);

            assertThat(entity.getItems().get(0).getBudget()).isSameAs(entity);
        }

        @Test
        @DisplayName("should default null paymentStatus to PENDING")
        void shouldDefaultNullPaymentStatusToPending() {
            BudgetItem item = BudgetItem.builder()
                    .id(UUID.randomUUID())
                    .tariffId(UUID.randomUUID())
                    .description("item")
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .paymentStatus(null)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .items(List.of(item))
                    .build();

            BudgetItemEntity itemEntity = mapper.toEntity(budget).getItems().get(0);

            assertThat(itemEntity.getPaymentStatus()).isEqualTo(ProcedurePaymentStatus.PENDING);
        }

        @Test
        @DisplayName("should default null discountPercentage to ZERO")
        void shouldDefaultNullDiscountPercentageToZero() {
            BudgetItem item = BudgetItem.builder()
                    .id(UUID.randomUUID())
                    .tariffId(UUID.randomUUID())
                    .description("item")
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .discountPercentage(null)
                    .paymentStatus(ProcedurePaymentStatus.PENDING)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .items(List.of(item))
                    .build();

            BudgetItemEntity itemEntity = mapper.toEntity(budget).getItems().get(0);

            assertThat(itemEntity.getDiscountPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle null items list without throwing")
        void shouldHandleNullItemsList() {
            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .items(null)
                    .build();

            BudgetEntity entity = mapper.toEntity(budget);

            assertThat(entity.getItems()).isEmpty();
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
        @DisplayName("should map all scalar fields from entity to domain")
        void shouldMapAllScalarFields() {
            UUID id = UUID.randomUUID();
            UUID patientId = UUID.randomUUID();
            UUID dentistId = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.now();

            BudgetEntity entity = BudgetEntity.builder()
                    .id(id)
                    .patientId(patientId)
                    .dentistId(dentistId)
                    .status(BudgetStatus.APPROVED)
                    .notes("note")
                    .createdAt(createdAt)
                    .items(List.of())
                    .build();

            Budget domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(id);
            assertThat(domain.getPatientId()).isEqualTo(patientId);
            assertThat(domain.getDentistId()).isEqualTo(dentistId);
            assertThat(domain.getStatus()).isEqualTo(BudgetStatus.APPROVED);
            assertThat(domain.getNotes()).isEqualTo("note");
            assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("should map item fields from entity to domain")
        void shouldMapItemFields() {
            UUID tariffId = UUID.randomUUID();
            BudgetEntity budgetEntity = BudgetEntity.builder()
                    .id(UUID.randomUUID())
                    .patientId(UUID.randomUUID())
                    .dentistId(UUID.randomUUID())
                    .status(BudgetStatus.DRAFT)
                    .createdAt(LocalDateTime.now())
                    .build();

            BudgetItemEntity itemEntity = BudgetItemEntity.builder()
                    .id(UUID.randomUUID())
                    .budget(budgetEntity)
                    .tariffId(tariffId)
                    .description("Extracción")
                    .quantity(1)
                    .unitPrice(new BigDecimal("80.00"))
                    .discountPercentage(new BigDecimal("5.00"))
                    .performed(false)
                    .paymentStatus(ProcedurePaymentStatus.PENDING)
                    .build();

            budgetEntity.getItems().add(itemEntity);

            Budget domain = mapper.toDomain(budgetEntity);

            assertThat(domain.getItems()).hasSize(1);
            BudgetItem item = domain.getItems().get(0);
            assertThat(item.getTariffId()).isEqualTo(tariffId);
            assertThat(item.getDescription()).isEqualTo("Extracción");
            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("80.00");
            assertThat(item.getDiscountPercentage()).isEqualByComparingTo("5.00");
            assertThat(item.getPerformed()).isFalse();
            assertThat(item.getPaymentStatus()).isEqualTo(ProcedurePaymentStatus.PENDING);
        }
    }
}
