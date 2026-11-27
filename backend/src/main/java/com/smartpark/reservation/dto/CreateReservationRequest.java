package com.smartpark.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

/** Primer: {"spotId": 5, "plateNumber": "NS-123-AB", "startAt": "2026-12-01T10:00:00+01:00", "endAt": "2026-12-01T12:00:00+01:00"} */
public record CreateReservationRequest(
        @NotNull(message = "Mesto je obavezno") Long spotId,
        @NotBlank(message = "Registarska oznaka je obavezna")
        @Pattern(regexp = "^[A-Za-z0-9ČĆŠĐŽčćšđž -]{2,15}$", message = "Neispravna registarska oznaka")
        String plateNumber,
        @NotNull(message = "Pocetak je obavezan") OffsetDateTime startAt,
        @NotNull(message = "Kraj je obavezan") OffsetDateTime endAt
) {
}
