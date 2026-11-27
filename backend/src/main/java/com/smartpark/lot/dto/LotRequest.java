package com.smartpark.lot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LotRequest(
        @NotBlank(message = "Naziv je obavezan") @Size(max = 100) String name,
        @NotBlank(message = "Adresa je obavezna") @Size(max = 255) String address,
        @Min(value = 1, message = "Kapacitet mora biti najmanje 1")
        @Max(value = 5000, message = "Kapacitet je prevelik") int capacity
) {
}
