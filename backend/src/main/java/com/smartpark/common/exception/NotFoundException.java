package com.smartpark.common.exception;

import org.springframework.http.HttpStatus;

/** Trazeni resurs ne postoji (404). */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
