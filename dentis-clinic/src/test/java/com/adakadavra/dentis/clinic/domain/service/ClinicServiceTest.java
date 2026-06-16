package com.adakadavra.dentis.clinic.domain.service;

import com.adakadavra.dentis.clinic.application.dto.ClinicResponse;
import com.adakadavra.dentis.clinic.application.dto.CreateClinicRequest;
import com.adakadavra.dentis.clinic.application.dto.UpdateClinicRequest;
import com.adakadavra.dentis.clinic.domain.model.Clinic;
import com.adakadavra.dentis.clinic.domain.repository.ClinicRepository;
import com.adakadavra.dentis.common.exception.BusinessRuleException;
import com.adakadavra.dentis.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicService")
class ClinicServiceTest {

    @Mock private ClinicRepository clinicRepository;

    @InjectMocks
    private ClinicService clinicService;

    private UUID clinicId;
    private Clinic activeClinic;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        activeClinic = Clinic.builder()
                .id(clinicId)
                .name("Clínica Dental Demo")
                .nif("J-12345678-9")
                .address("Av. Principal 123")
                .city("Caracas")
                .province("Miranda")
                .zipCode("1010")
                .phone("+58-212-5551234")
                .email("demo@clinicadental.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createClinic")
    class CreateClinic {

        @Test
        @DisplayName("should be created successfully when NIF is unique")
        void shouldBeCreatedWhenNifIsUnique() {
            CreateClinicRequest request = CreateClinicRequest.builder()
                    .name("Clínica Dental Demo")
                    .nif("J-12345678-9")
                    .city("Caracas")
                    .email("demo@clinicadental.com")
                    .build();

            when(clinicRepository.existsByNif("J-12345678-9")).thenReturn(false);
            when(clinicRepository.save(any())).thenReturn(activeClinic);

            ClinicResponse result = clinicService.createClinic(request);

            assertThat(result.getName()).isEqualTo("Clínica Dental Demo");
            assertThat(result.isActive()).isTrue();
            verify(clinicRepository).save(any());
        }

        @Test
        @DisplayName("should be created successfully when NIF is null")
        void shouldBeCreatedWhenNifIsNull() {
            CreateClinicRequest request = CreateClinicRequest.builder()
                    .name("Nueva Clínica")
                    .nif(null)
                    .city("Valencia")
                    .build();

            Clinic noNifClinic = activeClinic.toBuilder().nif(null).build();
            when(clinicRepository.save(any())).thenReturn(noNifClinic);

            ClinicResponse result = clinicService.createClinic(request);

            assertThat(result).isNotNull();
            verify(clinicRepository, never()).existsByNif(any());
        }

        @Test
        @DisplayName("should be rejected when NIF already belongs to another clinic")
        void shouldBeRejectedWhenNifAlreadyExists() {
            CreateClinicRequest request = CreateClinicRequest.builder()
                    .name("Duplicada")
                    .nif("J-12345678-9")
                    .build();

            when(clinicRepository.existsByNif("J-12345678-9")).thenReturn(true);

            assertThatThrownBy(() -> clinicService.createClinic(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("NIF already exists");

            verify(clinicRepository, never()).save(any());
        }

        @Test
        @DisplayName("should be created as active by default")
        void shouldBeCreatedAsActiveByDefault() {
            CreateClinicRequest request = CreateClinicRequest.builder()
                    .name("Nueva Clínica")
                    .build();

            when(clinicRepository.save(any())).thenAnswer(invocation -> {
                Clinic toSave = invocation.getArgument(0);
                assertThat(toSave.isActive()).isTrue();
                return activeClinic;
            });

            clinicService.createClinic(request);
        }
    }

    @Nested
    @DisplayName("getClinic")
    class GetClinic {

        @Test
        @DisplayName("should be returned when clinic exists")
        void shouldBeReturnedWhenExists() {
            when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(activeClinic));

            ClinicResponse result = clinicService.getClinic(clinicId);

            assertThat(result.getId()).isEqualTo(clinicId);
            assertThat(result.getName()).isEqualTo("Clínica Dental Demo");
        }

        @Test
        @DisplayName("should throw when clinic does not exist")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(clinicRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicService.getClinic(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllClinics")
    class GetAllClinics {

        @Test
        @DisplayName("should return paginated clinics")
        void shouldReturnPaginatedClinics() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Clinic> clinicPage = new PageImpl<>(List.of(activeClinic), pageable, 1);

            when(clinicRepository.findAll(pageable)).thenReturn(clinicPage);

            Page<ClinicResponse> result = clinicService.getAllClinics(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(clinicId);
        }

        @Test
        @DisplayName("should return empty page when no clinics exist")
        void shouldReturnEmptyPageWhenNone() {
            Pageable pageable = PageRequest.of(0, 10);
            when(clinicRepository.findAll(pageable)).thenReturn(Page.empty());

            Page<ClinicResponse> result = clinicService.getAllClinics(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllActiveClinics")
    class GetAllActiveClinics {

        @Test
        @DisplayName("should return only active clinics")
        void shouldReturnOnlyActiveClinics() {
            when(clinicRepository.findAllActive()).thenReturn(List.of(activeClinic));

            List<ClinicResponse> result = clinicService.getAllActiveClinics();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when no active clinics exist")
        void shouldReturnEmptyListWhenNone() {
            when(clinicRepository.findAllActive()).thenReturn(List.of());

            List<ClinicResponse> result = clinicService.getAllActiveClinics();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateClinic")
    class UpdateClinic {

        @Test
        @DisplayName("should be updated with new values when clinic exists")
        void shouldBeUpdatedWhenExists() {
            UpdateClinicRequest request = UpdateClinicRequest.builder()
                    .name("Clínica Actualizada")
                    .city("Maracaibo")
                    .email("nueva@clinica.com")
                    .build();

            Clinic updatedClinic = activeClinic.toBuilder()
                    .name("Clínica Actualizada")
                    .city("Maracaibo")
                    .email("nueva@clinica.com")
                    .build();

            when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(activeClinic));
            when(clinicRepository.save(any())).thenReturn(updatedClinic);

            ClinicResponse result = clinicService.updateClinic(clinicId, request);

            assertThat(result.getName()).isEqualTo("Clínica Actualizada");
            assertThat(result.getCity()).isEqualTo("Maracaibo");
            verify(clinicRepository).save(any());
        }

        @Test
        @DisplayName("should throw when clinic does not exist")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            UpdateClinicRequest request = UpdateClinicRequest.builder().name("X").build();

            when(clinicRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicService.updateClinic(unknownId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(clinicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateClinic")
    class DeactivateClinic {

        @Test
        @DisplayName("should be deactivated when clinic exists")
        void shouldBeDeactivatedWhenExists() {
            when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(activeClinic));

            clinicService.deactivateClinic(clinicId);

            verify(clinicRepository).deactivate(clinicId);
        }

        @Test
        @DisplayName("should throw when clinic does not exist")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(clinicRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clinicService.deactivateClinic(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(clinicRepository, never()).deactivate(any());
        }
    }
}
