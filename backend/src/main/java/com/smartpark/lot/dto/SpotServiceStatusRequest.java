package com.smartpark.lot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** FR-08: operater stavlja mesto van upotrebe (outOfService=true, reason obavezan) ili ga vraca. */
public record SpotServiceStatusRequest(
        @NotNull(message = "Polje outOfService je obavezno") Boolean outOfService,
        @Size(max = 255) String reason
) {
}
