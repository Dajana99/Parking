package com.smartpark.common.exception;

import org.springframework.http.HttpStatus;

/** Korisnik nema pravo nad ovim resursom (403), npr. tudja rezervacija. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
