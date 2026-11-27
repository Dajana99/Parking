package com.smartpark.lot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    List<ParkingLot> findAllByOrderByNameAsc();

    List<ParkingLot> findByActiveTrueOrderByNameAsc();
}
