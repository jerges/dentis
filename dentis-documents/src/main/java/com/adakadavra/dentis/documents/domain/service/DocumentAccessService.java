package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentAccessService {

    private final DocumentShareRepository shareRepository;

    public enum Role { SUPER_ADMIN, ADMIN, USER }

    /**
     * Puede el usuario VER esta carpeta.
     */
    public boolean canViewFolder(UUID userId, Role role, UUID userClinicId, DocumentFolder folder) {
        if (role == Role.SUPER_ADMIN) return true;
        if (!folder.getClinicId().equals(userClinicId)) return false;
        if (folder.getType() == FolderType.KNOWLEDGE_BASE) return role == Role.ADMIN;
        return isVisible(userId, ResourceType.FOLDER, folder.getId(), folder.getOwnerUserId(), folder.getVisibility());
    }

    /**
     * Puede el usuario GESTIONAR (renombrar, borrar, cambiar visibilidad) esta carpeta.
     */
    public boolean canManageFolder(UUID userId, Role role, UUID userClinicId, DocumentFolder folder) {
        if (role == Role.SUPER_ADMIN) return true;
        if (!folder.getClinicId().equals(userClinicId)) return false;
        if (folder.getType() == FolderType.KNOWLEDGE_BASE) return role == Role.ADMIN;
        return folder.getOwnerUserId().equals(userId);
    }

    /**
     * Puede el usuario VER este fichero.
     */
    public boolean canViewFile(UUID userId, Role role, UUID userClinicId, DocumentFile file) {
        if (role == Role.SUPER_ADMIN) return true;
        if (!file.getClinicId().equals(userClinicId)) return false;
        return isVisible(userId, ResourceType.FILE, file.getId(), file.getOwnerUserId(), file.getVisibility());
    }

    /**
     * Puede el usuario GESTIONAR (borrar, mover, cambiar visibilidad) este fichero.
     */
    public boolean canManageFile(UUID userId, Role role, UUID userClinicId, DocumentFile file) {
        if (role == Role.SUPER_ADMIN) return true;
        if (!file.getClinicId().equals(userClinicId)) return false;
        return file.getOwnerUserId().equals(userId);
    }

    private boolean isVisible(UUID userId, ResourceType type, UUID resourceId, UUID ownerUserId, Visibility visibility) {
        return switch (visibility) {
            case PUBLIC -> true;
            case PRIVATE -> ownerUserId.equals(userId);
            case SHARED -> ownerUserId.equals(userId) ||
                    shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(type, resourceId, userId);
        };
    }
}
