package com.smartpark.pricing.dto;

import com.smartpark.pricing.PriceList;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceListResponse(
        Long id,
        Long lotId,
        BigDecimal pricePerHour,
        BigDecimal minCharge,
        BigDecimal overstayFine,
        BigDecimal noShowFine,
        Instant validFrom
) {
    public static PriceListResponse from(PriceList p) {
        return new PriceListResponse(p.getId(), p.getLot().getId(), p.getPricePerHour(), p.getMinCharge(),
                p.getOverstayFine(), p.getNoShowFine(), p.getValidFrom());
    }
}
