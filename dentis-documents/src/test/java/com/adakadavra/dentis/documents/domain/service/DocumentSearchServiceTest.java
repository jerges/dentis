package com.adakadavra.dentis.documents.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchService")
class DocumentSearchServiceTest {

    @Mock private JdbcTemplate jdbc;

    @InjectMocks
    private DocumentSearchService documentSearchService;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should return matching results when records are found")
        void shouldReturnMatchingResults() {
            UUID clinicId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID fileId = UUID.randomUUID();
            UUID folderId = UUID.randomUUID();

            DocumentSearchService.SearchResult expected = new DocumentSearchService.SearchResult(
                    fileId, "Consentimiento.pdf", "FILE", folderId, "/documentos"
            );

            when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(List.of(expected));

            List<DocumentSearchService.SearchResult> results =
                    documentSearchService.search(clinicId, userId, "consentimiento");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("Consentimiento.pdf");
            assertThat(results.get(0).type()).isEqualTo("FILE");
        }

        @Test
        @DisplayName("should return empty list when no documents match")
        void shouldReturnEmptyListWhenNoMatch() {
            UUID clinicId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(List.of());

            List<DocumentSearchService.SearchResult> results =
                    documentSearchService.search(clinicId, userId, "xyz-not-found");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should pass clinic id and user id as query parameters for tenant isolation")
        void shouldPassClinicAndUserIdForTenantIsolation() {
            UUID clinicId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(List.of());

            documentSearchService.search(clinicId, userId, "radiografia");

            ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).query(anyString(), any(RowMapper.class), paramsCaptor.capture());

            Object[] params = paramsCaptor.getValue();
            assertThat(params).contains(clinicId);
            assertThat(params).contains(userId);
        }

        @Test
        @DisplayName("should include LIKE pattern with wildcards for the query term")
        void shouldIncludeLikePatternWithWildcards() {
            UUID clinicId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String query = "radiografia";

            when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(List.of());

            documentSearchService.search(clinicId, userId, query);

            ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).query(anyString(), any(RowMapper.class), paramsCaptor.capture());

            Object[] params = paramsCaptor.getValue();
            assertThat(params).contains("%" + query + "%");
        }

        @Test
        @DisplayName("should pass the query term multiple times for trigram and full-text")
        void shouldPassQueryTermMultipleTimesForDifferentSearchModes() {
            UUID clinicId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String query = "tratamiento";

            when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                    .thenReturn(List.of());

            documentSearchService.search(clinicId, userId, query);

            ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
            verify(jdbc).query(anyString(), any(RowMapper.class), paramsCaptor.capture());

            long queryOccurrences = java.util.Arrays.stream(paramsCaptor.getValue())
                    .filter(p -> query.equals(p))
                    .count();
            assertThat(queryOccurrences).isGreaterThanOrEqualTo(3);
        }
    }
}
