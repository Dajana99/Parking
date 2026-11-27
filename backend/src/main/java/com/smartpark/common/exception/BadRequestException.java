package com.smartpark.common.exception;

import org.springframework.http.HttpStatus;

/** Neispravan zahtev (400) koji Bean Validation ne moze da uhvati, npr. kraj pre pocetka. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
