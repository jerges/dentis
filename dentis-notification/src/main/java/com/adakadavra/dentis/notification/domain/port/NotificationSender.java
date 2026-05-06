package com.adakadavra.dentis.notification.domain.port;

import com.adakadavra.dentis.notification.domain.model.NotificationMessage;

public interface NotificationSender {

    void send(NotificationMessage message);
}
