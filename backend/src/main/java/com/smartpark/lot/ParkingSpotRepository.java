package com.smartpark.lot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByLotIdOrderBySpotNumberAsc(Long lotId);

    long countByLotId(Long lotId);

    boolean existsByLotIdAndSpotNumber(Long lotId, String spotNumber);

    /**
     * Zakljucava red mesta (SELECT ... FOR UPDATE) dok traje transakcija rezervacije,
     * tako da dva istovremena zahteva za isto mesto ne mogu oba da prodju proveru preklapanja.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSpot s join fetch s.lot where s.id = :id")
    Optional<ParkingSpot> findByIdForUpdate(@Param("id") Long id);

    /** Broj mesta po lokaciji i statusu - jedan upit za ceo spisak lokacija (FR-07). */
    @Query("select s.lot.id, s.status, count(s) from ParkingSpot s group by s.lot.id, s.status")
    List<Object[]> countByLotAndStatus();
}
