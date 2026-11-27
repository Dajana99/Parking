package com.smartpark.auth;

import com.smartpark.common.ApiException;
import org.springframework.http.HttpStatus;

/** Ista poruka i za nepostojeci e-mail i za pogresnu lozinku, da se ne otkriva koji nalozi postoje. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Pogresan e-mail ili lozinka.");
    }
}
