package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentFolderService")
class DocumentFolderServiceTest {

    @Mock
    private DocumentFolderRepository folderRepository;

    @Mock
    private DocumentShareRepository shareRepository;

    @InjectMocks
    private DocumentFolderService folderService;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    // ── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("crea y guarda una carpeta NORMAL con los campos correctos")
        void createsSavesFolder() {
            ArgumentCaptor<DocumentFolder> captor = ArgumentCaptor.forClass(DocumentFolder.class);
            when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DocumentFolder result = folderService.create(clinicId, userId, null, "Protocolos", Visibility.PUBLIC);

            verify(folderRepository).save(captor.capture());
            DocumentFolder saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Protocolos");
            assertThat(saved.getClinicId()).isEqualTo(clinicId);
            assertThat(saved.getOwnerUserId()).isEqualTo(userId);
            assertThat(saved.getType()).isEqualTo(FolderType.NORMAL);
            assertThat(saved.isSystem()).isFalse();
            assertThat(saved.getPath()).isEqualTo("/Protocolos");
            assertThat(result).isSameAs(saved);
        }
    }

    // ── getOrCreateKnowledgeBase ──────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreateKnowledgeBase")
    class GetOrCreateKnowledgeBase {

        @Test
        @DisplayName("retorna la KB existente si ya existe")
        void returnsExistingKb() {
            DocumentFolder existing = kbFolder();
            when(folderRepository.findByClinicIdAndType(clinicId, FolderType.KNOWLEDGE_BASE))
                    .thenReturn(Optional.of(existing));

            DocumentFolder result = folderService.getOrCreateKnowledgeBase(clinicId);

            assertThat(result).isSameAs(existing);
        }

        @Test
        @DisplayName("crea una KB de sistema cuando no existe")
        void createsKbWhenAbsent() {
            when(folderRepository.findByClinicIdAndType(clinicId, FolderType.KNOWLEDGE_BASE))
                    .thenReturn(Optional.empty());
            ArgumentCaptor<DocumentFolder> captor = ArgumentCaptor.forClass(DocumentFolder.class);
            when(folderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            folderService.getOrCreateKnowledgeBase(clinicId);

            verify(folderRepository).save(captor.capture());
            DocumentFolder created = captor.getValue();
            assertThat(created.getType()).isEqualTo(FolderType.KNOWLEDGE_BASE);
            assertThat(created.isSystem()).isTrue();
            assertThat(created.getClinicId()).isEqualTo(clinicId);
            assertThat(created.getName()).isEqualTo("Base de Conocimiento");
        }
    }

    // ── rename ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rename")
    class Rename {

        @Test
        @DisplayName("lanza IllegalStateException al intentar renombrar una carpeta de sistema")
        void throwsOnSystemFolder() {
            UUID id = UUID.randomUUID();
            when(folderRepository.findById(id)).thenReturn(Optional.of(kbFolder()));

            assertThatThrownBy(() -> folderService.rename(id, "NuevoNombre"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sistema");
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("lanza IllegalStateException al intentar borrar una carpeta de sistema")
        void throwsOnSystemFolder() {
            UUID id = UUID.randomUUID();
            when(folderRepository.findById(id)).thenReturn(Optional.of(kbFolder()));

            assertThatThrownBy(() -> folderService.delete(id))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sistema");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DocumentFolder kbFolder() {
        return DocumentFolder.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .name("Base de Conocimiento")
                .path("/Base de Conocimiento")
                .type(FolderType.KNOWLEDGE_BASE)
                .visibility(Visibility.PRIVATE)
                .system(true)
                .build();
    }
}
