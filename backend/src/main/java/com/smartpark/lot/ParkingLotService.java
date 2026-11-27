package com.smartpark.lot;

import com.smartpark.common.exception.ConflictException;
import com.smartpark.common.exception.NotFoundException;
import com.smartpark.lot.dto.LotRequest;
import com.smartpark.lot.dto.LotResponse;
import com.smartpark.reservation.ReservationRepository;
import com.smartpark.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FR-06 (administracija lokacija) i FR-07 (pregled sa brojem slobodnih mesta). */
@Service
public class ParkingLotService {

    private final ParkingLotRepository lotRepository;
    private final ParkingSpotRepository spotRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public ParkingLotService(ParkingLotRepository lotRepository,
                             ParkingSpotRepository spotRepository,
                             ReservationRepository reservationRepository,
                             Clock clock) {
        this.lotRepository = lotRepository;
        this.spotRepository = spotRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    /** Vozac vidi samo aktivne lokacije; administrator moze da trazi i deaktivirane. */
    @Transactional(readOnly = true)
    public List<LotResponse> list(boolean includeInactive) {
        List<ParkingLot> lots = includeInactive
                ? lotRepository.findAllByOrderByNameAsc()
                : lotRepository.findByActiveTrueOrderByNameAsc();

        Map<Long, Map<SpotStatus, Long>> counts = new HashMap<>();
        for (Object[] row : spotRepository.countByLotAndStatus()) {
            Long lotId = (Long) row[0];
            SpotStatus status = (SpotStatus) row[1];
            Long count = (Long) row[2];
            counts.computeIfAbsent(lotId, k -> new EnumMap<>(SpotStatus.class)).put(status, count);
        }
        return lots.stream()
                .map(lot -> toResponse(lot, counts.getOrDefault(lot.getId(), Map.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LotResponse get(Long lotId) {
        ParkingLot lot = getEntity(lotId);
        Map<SpotStatus, Long> byStatus = new EnumMap<>(SpotStatus.class);
        for (ParkingSpot s : spotRepository.findByLotIdOrderBySpotNumberAsc(lotId)) {
            byStatus.merge(s.getStatus(), 1L, Long::sum);
        }
        return toResponse(lot, byStatus);
    }

    @Transactional
    public LotResponse create(LotRequest req) {
        ParkingLot lot = new ParkingLot();
        lot.setName(req.name().trim());
        lot.setAddress(req.address().trim());
        lot.setCapacity(req.capacity());
        lot.setActive(true);
        return toResponse(lotRepository.save(lot), Map.of());
    }

    @Transactional
    public LotResponse update(Long lotId, LotRequest req) {
        ParkingLot lot = getEntity(lotId);
        long existingSpots = spotRepository.countByLotId(lotId);
        if (req.capacity() < existingSpots) {
            throw new ConflictException("Kapacitet (" + req.capacity()
                    + ") ne moze biti manji od broja postojecih mesta (" + existingSpots + ")");
        }
        lot.setName(req.name().trim());
        lot.setAddress(req.address().trim());
        lot.setCapacity(req.capacity());
        return get(lot.getId());
    }

    /** Deaktivacija umesto brisanja - istorija rezervacija i izvestaji ostaju ispravni. */
    @Transactional
    public void deactivate(Long lotId) {
        ParkingLot lot = getEntity(lotId);
        Instant now = clock.instant();
        if (reservationRepository.existsLiveOnLot(lotId, ReservationStatus.BLOCKING, now)) {
            throw new ConflictException("Lokacija ima aktivne ili buduce rezervacije i ne moze se deaktivirati");
        }
        lot.setActive(false);
    }

    @Transactional
    public void activate(Long lotId) {
        getEntity(lotId).setActive(true);
    }

    public ParkingLot getEntity(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new NotFoundException("Lokacija " + lotId + " ne postoji"));
    }

    private LotResponse toResponse(ParkingLot lot, Map<SpotStatus, Long> byStatus) {
        long free = byStatus.getOrDefault(SpotStatus.SLOBODNO, 0L);
        long reserved = byStatus.getOrDefault(SpotStatus.REZERVISANO, 0L);
        long occupied = byStatus.getOrDefault(SpotStatus.ZAUZETO, 0L);
        long outOfService = byStatus.getOrDefault(SpotStatus.VAN_UPOTREBE, 0L);
        return new LotResponse(lot.getId(), lot.getName(), lot.getAddress(), lot.getCapacity(),
                lot.isActive(), free + reserved + occupied + outOfService,
                free, reserved, occupied, outOfService);
    }
}
