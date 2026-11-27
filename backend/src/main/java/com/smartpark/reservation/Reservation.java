package com.smartpark.reservation;

import com.smartpark.lot.ParkingSpot;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    /** Cuva se samo ID (bez veze na User entitet) - pri brisanju naloga postaje NULL. */
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @Column(name = "plate_number", nullable = false, length = 15)
    private String plateNumber;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ReservationStatus status = ReservationStatus.KREIRANA;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Menja stanje uz proveru dozvoljenog prelaza. Ne postoji obican setStatus namerno. */
    public void transitionTo(ReservationStatus target) {
        ReservationStateMachine.assertTransition(this.status, target);
        this.status = target;
    }

    public boolean overlaps(Instant from, Instant to) {
        return startAt.isBefore(to) && endAt.isAfter(from);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ParkingSpot getSpot() { return spot; }
    public void setSpot(ParkingSpot spot) { this.spot = spot; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
