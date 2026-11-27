package com.smartpark.common.exception;

import org.springframework.http.HttpStatus;

/** Zahtev je u sukobu sa trenutnim stanjem (409), npr. dvostruka rezervacija. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
