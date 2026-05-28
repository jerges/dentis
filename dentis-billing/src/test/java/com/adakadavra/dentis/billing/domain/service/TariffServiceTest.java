package com.adakadavra.dentis.billing.domain.service;

import com.adakadavra.dentis.billing.domain.model.Tariff;
import com.adakadavra.dentis.billing.domain.model.TariffCategory;
import com.adakadavra.dentis.billing.domain.repository.TariffRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TariffService")
class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @InjectMocks
    private TariffService tariffService;

    @Nested
    @DisplayName("createTariff")
    class CreateTariff {

        @Test
        @DisplayName("should be created when code is unique")
        void shouldBeCreatedWhenCodeIsUnique() {
            // Arrange
            Tariff tariff = buildTariff("CLEAN-001", "Dental cleaning");
            when(tariffRepository.existsByCode("CLEAN-001")).thenReturn(false);
            when(tariffRepository.save(tariff)).thenReturn(tariff);

            // Act
            Tariff created = tariffService.createTariff(tariff);

            // Assert
            assertThat(created).isEqualTo(tariff);
            verify(tariffRepository).save(tariff);
        }

        @Test
        @DisplayName("should be rejected when code already exists")
        void shouldBeRejectedWhenCodeAlreadyExists() {
            // Arrange
            Tariff tariff = buildTariff("CLEAN-001", "Dental cleaning");
            when(tariffRepository.existsByCode("CLEAN-001")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> tariffService.createTariff(tariff))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("CLEAN-001");
            verify(tariffRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("should be returned as page when listing tariffs")
    void shouldBeReturnedAsPageWhenListingTariffs() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tariff> expectedPage = new PageImpl<>(List.of(buildTariff("CONSULT-001", "Consultation")), pageable, 1);
        when(tariffRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<Tariff> result = tariffService.findAll(pageable);

        // Assert
        assertThat(result).isEqualTo(expectedPage);
        verify(tariffRepository).findAll(pageable);
    }

    @Nested
    @DisplayName("deactivateTariff")
    class DeactivateTariff {

        @Test
        @DisplayName("should be deactivated when tariff exists")
        void shouldBeDeactivatedWhenTariffExists() {
            // Arrange
            UUID tariffId = UUID.randomUUID();
            when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(buildTariff("XRAY-001", "X-Ray")));

            // Act
            tariffService.deactivateTariff(tariffId);

            // Assert
            verify(tariffRepository).deactivate(tariffId);
        }

        @Test
        @DisplayName("should be rejected when tariff does not exist")
        void shouldBeRejectedWhenTariffDoesNotExist() {
            // Arrange
            UUID tariffId = UUID.randomUUID();
            when(tariffRepository.findById(tariffId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> tariffService.deactivateTariff(tariffId))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(tariffRepository, never()).deactivate(any());
        }
    }

    private Tariff buildTariff(String code, String name) {
        return Tariff.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .description(name)
                .category(TariffCategory.GENERAL_DENTISTRY)
                .basePrice(BigDecimal.valueOf(25))
                .discountAllowed(false)
                .active(true)
                .build();
    }
}

