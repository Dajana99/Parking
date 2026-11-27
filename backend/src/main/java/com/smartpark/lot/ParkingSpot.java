package com.smartpark.lot;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_spots")
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    @Column(name = "spot_number", nullable = false, length = 20)
    private String spotNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpotStatus status = SpotStatus.SLOBODNO;

    @Column(name = "out_of_service_reason")
    private String outOfServiceReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ParkingLot getLot() { return lot; }
    public void setLot(ParkingLot lot) { this.lot = lot; }
    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }
    public SpotStatus getStatus() { return status; }
    public void setStatus(SpotStatus status) { this.status = status; }
    public String getOutOfServiceReason() { return outOfServiceReason; }
    public void setOutOfServiceReason(String outOfServiceReason) { this.outOfServiceReason = outOfServiceReason; }
}
