package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentSearchService {

    private final JdbcTemplate jdbc;

    public record SearchResult(UUID id, String name, String type, UUID folderId, String folderPath) {}

    /**
     * Búsqueda híbrida: trigrama (nombre) + full-text (contenido) acotada a la clínica.
     * Devuelve ficheros ordenados por relevancia.
     */
    public List<SearchResult> search(UUID clinicId, UUID userId, String query) {
        String sql = """
                SELECT f.id, f.name, 'FILE' AS type, f.folder_id,
                       fol.path AS folder_path
                FROM document_files f
                JOIN document_folders fol ON fol.id = f.folder_id
                WHERE f.clinic_id = ?
                  AND (
                        f.name ILIKE ? OR
                        f.tsv @@ plainto_tsquery('spanish', ?) OR
                        f.name % ?
                      )
                  AND (
                        f.visibility = 'PUBLIC'
                     OR f.owner_user_id = ?
                     OR (f.visibility = 'SHARED' AND EXISTS (
                            SELECT 1 FROM document_shares s
                            WHERE s.resource_type = 'FILE'
                              AND s.resource_id = f.id
                              AND s.shared_with_user_id = ?
                         ))
                      )
                ORDER BY
                  ts_rank(f.tsv, plainto_tsquery('spanish', ?)) DESC,
                  similarity(f.name, ?) DESC
                LIMIT 20
                """;

        String like = "%" + query + "%";
        return jdbc.query(sql,
                (rs, i) -> new SearchResult(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name"),
                        rs.getString("type"),
                        UUID.fromString(rs.getString("folder_id")),
                        rs.getString("folder_path")
                ),
                clinicId, like, query, query, userId, userId, query, query);
    }
}
