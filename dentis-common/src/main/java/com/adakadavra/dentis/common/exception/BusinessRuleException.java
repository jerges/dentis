package com.adakadavra.dentis.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DentisException {

    public BusinessRuleException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }
}
