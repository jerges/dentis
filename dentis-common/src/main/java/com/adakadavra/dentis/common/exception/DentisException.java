package com.adakadavra.dentis.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DentisException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public DentisException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public DentisException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
