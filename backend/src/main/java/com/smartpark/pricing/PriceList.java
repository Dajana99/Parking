package com.smartpark.pricing;

import com.smartpark.lot.ParkingLot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Jedna verzija cenovnika za lokaciju (FR-15). Izmena cenovnika pravi NOVU verziju sa valid_from,
 * tako da stari obracuni i izvestaji ostaju tacni.
 */
@Entity
@Table(name = "price_lists")
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "min_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal minCharge;

    @Column(name = "overstay_fine", nullable = false, precision = 10, scale = 2)
    private BigDecimal overstayFine;

    @Column(name = "no_show_fine", nullable = false, precision = 10, scale = 2)
    private BigDecimal noShowFine;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ParkingLot getLot() { return lot; }
    public void setLot(ParkingLot lot) { this.lot = lot; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public BigDecimal getMinCharge() { return minCharge; }
    public void setMinCharge(BigDecimal minCharge) { this.minCharge = minCharge; }
    public BigDecimal getOverstayFine() { return overstayFine; }
    public void setOverstayFine(BigDecimal overstayFine) { this.overstayFine = overstayFine; }
    public BigDecimal getNoShowFine() { return noShowFine; }
    public void setNoShowFine(BigDecimal noShowFine) { this.noShowFine = noShowFine; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
}
