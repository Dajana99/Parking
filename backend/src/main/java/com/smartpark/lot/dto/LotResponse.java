package com.smartpark.lot.dto;

public record LotResponse(
        Long id,
        String name,
        String address,
        int capacity,
        boolean active,
        long totalSpots,
        long freeSpots,
        long reservedSpots,
        long occupiedSpots,
        long outOfServiceSpots
) {
}
