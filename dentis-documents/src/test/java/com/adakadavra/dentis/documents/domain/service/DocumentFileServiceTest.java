package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentFileRepository;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentFileService")
class DocumentFileServiceTest {

    @Mock
    private DocumentFileRepository fileRepository;

    @Mock
    private DocumentShareRepository shareRepository;

    @InjectMocks
    private DocumentFileService fileService;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    // ── move ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("move")
    class Move {

        @Test
        @DisplayName("mover un fichero resetea indexedForIa a false")
        void moveResetsIndexedForIa() {
            UUID fileId = UUID.randomUUID();
            UUID targetFolderId = UUID.randomUUID();
            DocumentFile file = indexedFile(fileId);
            when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DocumentFile result = fileService.move(fileId, targetFolderId);

            assertThat(result.isIndexedForIa()).isFalse();
            assertThat(result.getFolderId()).isEqualTo(targetFolderId);
        }
    }

    // ── markIndexed ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markIndexed")
    class MarkIndexed {

        @Test
        @DisplayName("actualiza indexedForIa a true")
        void setsIndexedFlagToTrue() {
            UUID fileId = UUID.randomUUID();
            DocumentFile file = notIndexedFile(fileId);
            when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DocumentFile result = fileService.markIndexed(fileId);

            assertThat(result.isIndexedForIa()).isTrue();
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("elimina el fichero y sus shares por ID")
        void deletesFileAndShares() {
            UUID fileId = UUID.randomUUID();
            DocumentFile file = notIndexedFile(fileId);
            when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

            fileService.delete(fileId);

            verify(fileRepository).deleteById(fileId);
        }
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("registra un fichero con indexedForIa=false por defecto")
        void registersWithIndexedFalse() {
            ArgumentCaptor<DocumentFile> captor = ArgumentCaptor.forClass(DocumentFile.class);
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            fileService.register(clinicId, folderId, userId, "Protocolo", "protocolo.pdf",
                    "application/pdf", 1024L, "clinic/docs/protocolo.pdf", Visibility.PRIVATE);

            verify(fileRepository).save(captor.capture());
            DocumentFile saved = captor.getValue();
            assertThat(saved.isIndexedForIa()).isFalse();
            assertThat(saved.getName()).isEqualTo("Protocolo");
            assertThat(saved.getClinicId()).isEqualTo(clinicId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DocumentFile indexedFile(UUID id) {
        return DocumentFile.builder()
                .id(id)
                .clinicId(clinicId)
                .folderId(folderId)
                .ownerUserId(userId)
                .name("doc.pdf")
                .fileName("doc.pdf")
                .indexedForIa(true)
                .visibility(Visibility.PRIVATE)
                .build();
    }

    private DocumentFile notIndexedFile(UUID id) {
        return DocumentFile.builder()
                .id(id)
                .clinicId(clinicId)
                .folderId(folderId)
                .ownerUserId(userId)
                .name("doc.pdf")
                .fileName("doc.pdf")
                .indexedForIa(false)
                .visibility(Visibility.PRIVATE)
                .build();
    }
}
