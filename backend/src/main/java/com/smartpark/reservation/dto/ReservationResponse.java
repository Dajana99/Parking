package com.smartpark.reservation.dto;

import com.smartpark.reservation.Reservation;
import com.smartpark.reservation.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long userId,
        Long lotId,
        String lotName,
        Long spotId,
        String spotNumber,
        String plateNumber,
        Instant startAt,
        Instant endAt,
        ReservationStatus status,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getUserId(),
                r.getSpot().getLot().getId(),
                r.getSpot().getLot().getName(),
                r.getSpot().getId(),
                r.getSpot().getSpotNumber(),
                r.getPlateNumber(),
                r.getStartAt(),
                r.getEndAt(),
                r.getStatus(),
                r.getCreatedAt());
    }
}
