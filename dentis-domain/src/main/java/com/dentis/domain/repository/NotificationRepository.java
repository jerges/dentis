package com.dentis.domain.repository;

import com.dentis.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findBySentFalseAndRetryCountLessThan(int maxRetries);
}
