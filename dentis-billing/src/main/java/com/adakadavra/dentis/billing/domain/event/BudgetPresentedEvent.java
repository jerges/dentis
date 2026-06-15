package com.adakadavra.dentis.billing.domain.event;

import com.adakadavra.dentis.billing.domain.model.Budget;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BudgetPresentedEvent extends ApplicationEvent {

    private final Budget budget;

    public BudgetPresentedEvent(Object source, Budget budget) {
        super(source);
        this.budget = budget;
    }
}
