package com.smartpark.common;

import org.springframework.http.HttpStatus;

/**
 * Osnovna klasa za poslovne greske koje treba da postanu HTTP odgovor sa porukom.
 * Kasnije je koristite i za 409 kod nedozvoljenih prelaza stanja (FR-12).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
