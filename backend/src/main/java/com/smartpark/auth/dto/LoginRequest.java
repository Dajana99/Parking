package com.smartpark.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-mail je obavezan.")
        @Email(message = "E-mail nije ispravan.")
        String email,

        @NotBlank(message = "Lozinka je obavezna.")
        String password
) {
}
