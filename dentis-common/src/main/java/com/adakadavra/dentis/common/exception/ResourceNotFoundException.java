package com.adakadavra.dentis.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DentisException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(
                String.format("%s with id '%s' not found", resourceName, id),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }
}
