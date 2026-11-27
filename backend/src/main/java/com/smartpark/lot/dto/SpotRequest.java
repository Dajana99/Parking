package com.smartpark.lot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SpotRequest(
        @NotBlank(message = "Oznaka mesta je obavezna")
        @Pattern(regexp = "^[A-Za-z0-9-]{1,20}$", message = "Oznaka sme da sadrzi slova, brojeve i crticu (do 20)")
        String spotNumber
) {
}
