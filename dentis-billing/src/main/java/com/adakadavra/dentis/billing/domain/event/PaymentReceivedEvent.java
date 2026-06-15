package com.adakadavra.dentis.billing.domain.event;

import com.adakadavra.dentis.billing.domain.model.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentReceivedEvent extends ApplicationEvent {

    private final Payment payment;

    public PaymentReceivedEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
