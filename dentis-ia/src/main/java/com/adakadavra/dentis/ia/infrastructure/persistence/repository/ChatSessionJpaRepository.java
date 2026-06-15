package com.adakadavra.dentis.ia.infrastructure.persistence.repository;

import com.adakadavra.dentis.ia.infrastructure.persistence.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSessionEntity, UUID> {

    List<ChatSessionEntity> findByDentistIdOrderByUpdatedAtDesc(UUID dentistId);

    List<ChatSessionEntity> findByClinicIdOrderByUpdatedAtDesc(UUID clinicId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatSessionEntity s SET s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    void touchUpdatedAt(@Param("id") UUID id);

    @Query(value = """
            SELECT u.username, COUNT(m.id) as message_count, SUM(m.input_tokens) as total_input,
                   SUM(m.output_tokens) as total_output
            FROM ia_chat_sessions s
            JOIN ia_chat_messages m ON m.session_id = s.id
            JOIN users u ON u.id = s.dentist_id
            WHERE s.clinic_id = :clinicId
            GROUP BY u.username, s.dentist_id
            ORDER BY message_count DESC
            """, nativeQuery = true)
    List<Object[]> getUsageByClinic(@Param("clinicId") UUID clinicId);

    @Query(value = """
            SELECT u.username, c.name as clinic_name, COUNT(m.id) as message_count,
                   SUM(m.input_tokens) as total_input, SUM(m.output_tokens) as total_output
            FROM ia_chat_sessions s
            JOIN ia_chat_messages m ON m.session_id = s.id
            JOIN users u ON u.id = s.dentist_id
            JOIN clinics c ON c.id = s.clinic_id
            GROUP BY u.username, c.name
            ORDER BY message_count DESC
            """, nativeQuery = true)
    List<Object[]> getUsageAll();

    long countByClinicId(UUID clinicId);
}
