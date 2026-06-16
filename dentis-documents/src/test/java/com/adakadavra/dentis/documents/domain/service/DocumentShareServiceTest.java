package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentShare;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentShareService")
class DocumentShareServiceTest {

    @Mock private DocumentShareRepository shareRepository;

    @InjectMocks
    private DocumentShareService documentShareService;

    private UUID resourceId;
    private UUID sharedWithUserId;
    private DocumentShare existingShare;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        sharedWithUserId = UUID.randomUUID();
        existingShare = DocumentShare.builder()
                .id(UUID.randomUUID())
                .resourceType(ResourceType.FILE)
                .resourceId(resourceId)
                .sharedWithUserId(sharedWithUserId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("share")
    class Share {

        @Test
        @DisplayName("should be created and saved when share does not exist yet")
        void shouldBeCreatedWhenShareDoesNotExist() {
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FILE, resourceId, sharedWithUserId)).thenReturn(false);
            when(shareRepository.save(any())).thenReturn(existingShare);

            DocumentShare result = documentShareService.share(ResourceType.FILE, resourceId, sharedWithUserId);

            assertThat(result).isNotNull();
            verify(shareRepository).save(any());
        }

        @Test
        @DisplayName("should return existing share without saving when share already exists")
        void shouldReturnExistingShareWithoutSaving() {
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FILE, resourceId, sharedWithUserId)).thenReturn(true);
            when(shareRepository.findByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FILE, resourceId, sharedWithUserId)).thenReturn(Optional.of(existingShare));

            DocumentShare result = documentShareService.share(ResourceType.FILE, resourceId, sharedWithUserId);

            assertThat(result.getId()).isEqualTo(existingShare.getId());
            verify(shareRepository, never()).save(any());
        }

        @Test
        @DisplayName("should work for FOLDER resource type")
        void shouldWorkForFolderResourceType() {
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FOLDER, resourceId, sharedWithUserId)).thenReturn(false);
            DocumentShare folderShare = DocumentShare.builder()
                    .id(existingShare.getId())
                    .resourceType(ResourceType.FOLDER)
                    .resourceId(existingShare.getResourceId())
                    .sharedWithUserId(existingShare.getSharedWithUserId())
                    .createdAt(existingShare.getCreatedAt())
                    .build();
            when(shareRepository.save(any())).thenReturn(folderShare);

            DocumentShare result = documentShareService.share(ResourceType.FOLDER, resourceId, sharedWithUserId);

            assertThat(result).isNotNull();
            ArgumentCaptor<DocumentShare> captor = ArgumentCaptor.forClass(DocumentShare.class);
            verify(shareRepository).save(captor.capture());
            assertThat(captor.getValue().getResourceType()).isEqualTo(ResourceType.FOLDER);
        }

        @Test
        @DisplayName("should set an id and createdAt when creating a new share")
        void shouldSetIdAndCreatedAtOnNewShare() {
            when(shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                    ResourceType.FILE, resourceId, sharedWithUserId)).thenReturn(false);
            when(shareRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            documentShareService.share(ResourceType.FILE, resourceId, sharedWithUserId);

            ArgumentCaptor<DocumentShare> captor = ArgumentCaptor.forClass(DocumentShare.class);
            verify(shareRepository).save(captor.capture());
            DocumentShare saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("listShares")
    class ListShares {

        @Test
        @DisplayName("should return all shares for the resource")
        void shouldReturnAllSharesForResource() {
            when(shareRepository.findByResourceTypeAndResourceId(ResourceType.FILE, resourceId))
                    .thenReturn(List.of(existingShare));

            List<DocumentShare> result = documentShareService.listShares(ResourceType.FILE, resourceId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getResourceId()).isEqualTo(resourceId);
        }

        @Test
        @DisplayName("should return empty list when no shares exist")
        void shouldReturnEmptyListWhenNone() {
            when(shareRepository.findByResourceTypeAndResourceId(ResourceType.FILE, resourceId))
                    .thenReturn(List.of());

            List<DocumentShare> result = documentShareService.listShares(ResourceType.FILE, resourceId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeShare")
    class RemoveShare {

        @Test
        @DisplayName("should delete the share by id")
        void shouldDeleteShareById() {
            UUID shareId = existingShare.getId();

            documentShareService.removeShare(shareId);

            verify(shareRepository).deleteById(shareId);
        }
    }
}
