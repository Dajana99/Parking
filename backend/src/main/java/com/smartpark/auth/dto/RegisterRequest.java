package com.smartpark.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-01: ime, e-mail, lozinka. */
public record RegisterRequest(
        @NotBlank(message = "Ime je obavezno.")
        @Size(max = 100, message = "Ime moze imati najvise 100 karaktera.")
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Ime sme da sadrzi samo slova, razmak, tacku, apostrof i crticu.")
        String name,

        @NotBlank(message = "E-mail je obavezan.")
        @Email(message = "E-mail nije ispravan.")
        @Size(max = 255, message = "E-mail moze imati najvise 255 karaktera.")
        String email,

        // BCrypt koristi najvise 72 bajta lozinke, pa je to gornja granica
        @NotBlank(message = "Lozinka je obavezna.")
        @Size(min = 8, max = 72, message = "Lozinka mora imati izmedju 8 i 72 karaktera.")
        String password
) {
}
