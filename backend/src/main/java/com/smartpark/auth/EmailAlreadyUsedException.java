package com.smartpark.auth;

import com.smartpark.common.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "Nalog sa ovom e-mail adresom vec postoji.");
    }
}
