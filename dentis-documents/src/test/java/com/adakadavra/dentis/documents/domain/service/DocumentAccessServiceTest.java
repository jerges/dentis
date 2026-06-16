package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentAccessService")
class DocumentAccessServiceTest {

    @Mock
    private DocumentShareRepository shareRepository;

    @InjectMocks
    private DocumentAccessService accessService;

    private final UUID clinicId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    // ── canViewFolder ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canViewFolder")
    class CanViewFolder {

        @Test
        @DisplayName("SUPER_ADMIN puede ver cualquier carpeta sin importar la clínica")
        void superAdminSeeCrossClinic() {
            DocumentFolder folder = normalFolder(UUID.randomUUID(), otherUserId, Visibility.PRIVATE);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.SUPER_ADMIN, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("ADMIN puede ver carpeta KNOWLEDGE_BASE de su clínica")
        void adminSeesKb() {
            DocumentFolder kb = kbFolder(clinicId, null);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.ADMIN, clinicId, kb))
                    .isTrue();
        }

        @Test
        @DisplayName("USER no puede ver carpeta KNOWLEDGE_BASE")
        void userCannotSeeKb() {
            DocumentFolder kb = kbFolder(clinicId, null);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, kb))
                    .isFalse();
        }

        @Test
        @DisplayName("cross-clinic siempre denegado para no SUPER_ADMIN")
        void crossClinicDenied() {
            DocumentFolder folder = normalFolder(UUID.randomUUID(), otherUserId, Visibility.PUBLIC);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.ADMIN, clinicId, folder))
                    .isFalse();
        }

        @Test
        @DisplayName("PUBLIC visible para cualquier usuario de la clínica")
        void publicVisibleToAll() {
            DocumentFolder folder = normalFolder(clinicId, otherUserId, Visibility.PUBLIC);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("PRIVATE visible solo para el owner")
        void privateVisibleToOwnerOnly() {
            DocumentFolder folder = normalFolder(clinicId, userId, Visibility.PRIVATE);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("PRIVATE no visible para otro usuario de la clínica")
        void privateNotVisibleToOther() {
            DocumentFolder folder = normalFolder(clinicId, otherUserId, Visibility.PRIVATE);
            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isFalse();
        }

        @Test
        @DisplayName("SHARED visible para el usuario cuando existe share")
        void sharedVisibleWhenShareExists() {
            UUID folderId = UUID.randomUUID();
            DocumentFolder folder = folderWith(clinicId, folderId, otherUserId, Visibility.SHARED, FolderType.NORMAL, false);
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FOLDER, folderId, userId)).thenReturn(true);

            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("SHARED no visible cuando no hay share para el usuario")
        void sharedNotVisibleWithoutShare() {
            UUID folderId = UUID.randomUUID();
            DocumentFolder folder = folderWith(clinicId, folderId, otherUserId, Visibility.SHARED, FolderType.NORMAL, false);
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FOLDER, folderId, userId)).thenReturn(false);

            assertThat(accessService.canViewFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isFalse();
        }
    }

    // ── canManageFolder ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("canManageFolder")
    class CanManageFolder {

        @Test
        @DisplayName("SUPER_ADMIN puede gestionar cualquier carpeta")
        void superAdminManageAll() {
            DocumentFolder folder = normalFolder(UUID.randomUUID(), otherUserId, Visibility.PRIVATE);
            assertThat(accessService.canManageFolder(userId, DocumentAccessService.Role.SUPER_ADMIN, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("ADMIN puede gestionar carpeta KB de su clínica")
        void adminManagesKb() {
            DocumentFolder kb = kbFolder(clinicId, null);
            assertThat(accessService.canManageFolder(userId, DocumentAccessService.Role.ADMIN, clinicId, kb))
                    .isTrue();
        }

        @Test
        @DisplayName("USER no puede gestionar carpeta KB")
        void userCannotManageKb() {
            DocumentFolder kb = kbFolder(clinicId, null);
            assertThat(accessService.canManageFolder(userId, DocumentAccessService.Role.USER, clinicId, kb))
                    .isFalse();
        }

        @Test
        @DisplayName("owner puede gestionar su carpeta NORMAL")
        void ownerManagesOwnFolder() {
            DocumentFolder folder = normalFolder(clinicId, userId, Visibility.PRIVATE);
            assertThat(accessService.canManageFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isTrue();
        }

        @Test
        @DisplayName("no owner no puede gestionar carpeta NORMAL ajena")
        void nonOwnerCannotManage() {
            DocumentFolder folder = normalFolder(clinicId, otherUserId, Visibility.PUBLIC);
            assertThat(accessService.canManageFolder(userId, DocumentAccessService.Role.USER, clinicId, folder))
                    .isFalse();
        }
    }

    // ── canViewFile ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canViewFile")
    class CanViewFile {

        @Test
        @DisplayName("SUPER_ADMIN puede ver cualquier fichero")
        void superAdminSeesAll() {
            DocumentFile file = fileWith(UUID.randomUUID(), otherUserId, Visibility.PRIVATE);
            assertThat(accessService.canViewFile(userId, DocumentAccessService.Role.SUPER_ADMIN, clinicId, file))
                    .isTrue();
        }

        @Test
        @DisplayName("PUBLIC visible para cualquier usuario de la clínica")
        void publicVisible() {
            DocumentFile file = fileWith(clinicId, otherUserId, Visibility.PUBLIC);
            assertThat(accessService.canViewFile(userId, DocumentAccessService.Role.USER, clinicId, file))
                    .isTrue();
        }

        @Test
        @DisplayName("PRIVATE visible solo para el owner")
        void privateVisibleToOwner() {
            DocumentFile file = fileWith(clinicId, userId, Visibility.PRIVATE);
            assertThat(accessService.canViewFile(userId, DocumentAccessService.Role.USER, clinicId, file))
                    .isTrue();
        }

        @Test
        @DisplayName("cross-clinic siempre denegado")
        void crossClinicDenied() {
            DocumentFile file = fileWith(UUID.randomUUID(), otherUserId, Visibility.PUBLIC);
            assertThat(accessService.canViewFile(userId, DocumentAccessService.Role.USER, clinicId, file))
                    .isFalse();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DocumentFolder normalFolder(UUID folderClinicId, UUID owner, Visibility vis) {
        return folderWith(folderClinicId, UUID.randomUUID(), owner, vis, FolderType.NORMAL, false);
    }

    private DocumentFolder kbFolder(UUID folderClinicId, UUID owner) {
        return folderWith(folderClinicId, UUID.randomUUID(), owner, Visibility.PRIVATE, FolderType.KNOWLEDGE_BASE, true);
    }

    private DocumentFolder folderWith(UUID folderClinicId, UUID id, UUID owner,
                                       Visibility vis, FolderType type, boolean system) {
        return DocumentFolder.builder()
                .id(id)
                .clinicId(folderClinicId)
                .ownerUserId(owner)
                .visibility(vis)
                .type(type)
                .system(system)
                .name("Test Folder")
                .path("/Test Folder")
                .build();
    }

    private DocumentFile fileWith(UUID fileClinicId, UUID owner, Visibility vis) {
        return DocumentFile.builder()
                .id(UUID.randomUUID())
                .clinicId(fileClinicId)
                .ownerUserId(owner)
                .visibility(vis)
                .name("test.pdf")
                .fileName("test.pdf")
                .build();
    }
}
