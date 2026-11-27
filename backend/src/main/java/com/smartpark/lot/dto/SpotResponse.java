package com.smartpark.lot.dto;

import com.smartpark.lot.SpotStatus;

/**
 * availableInInterval je popunjen samo kad vozac pita za interval (from, to);
 * inace je null.
 */
public record SpotResponse(
        Long id,
        Long lotId,
        String spotNumber,
        SpotStatus status,
        String outOfServiceReason,
        Boolean availableInInterval
) {
}
