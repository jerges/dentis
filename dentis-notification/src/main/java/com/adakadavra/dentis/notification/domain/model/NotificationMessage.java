package com.adakadavra.dentis.notification.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NotificationMessage {

    private final String recipientEmail;
    private final String recipientName;
    private final NotificationType type;
    private final Map<String, Object> variables;
}
