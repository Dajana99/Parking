package com.smartpark.lot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** Pravi vise mesta odjednom: prefix "A", count 10 -> A1 ... A10. */
public record BulkSpotsRequest(
        @Pattern(regexp = "^[A-Za-z]{0,5}$", message = "Prefiks sme da ima do 5 slova") String prefix,
        @Min(1) @Max(500) int count
) {
}
