package com.smartpark.lot;

import com.smartpark.common.exception.BadRequestException;
import com.smartpark.common.exception.ConflictException;
import com.smartpark.common.exception.NotFoundException;
import com.smartpark.lot.dto.BulkSpotsRequest;
import com.smartpark.lot.dto.SpotResponse;
import com.smartpark.lot.dto.SpotServiceStatusRequest;
import com.smartpark.reservation.ReservationRepository;
import com.smartpark.reservation.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** FR-06 (mesta), FR-07 (status mesta i dostupnost u intervalu), FR-08 (van upotrebe). */
@Service
public class ParkingSpotService {

    private static final Set<ReservationStatus> IN_PROGRESS =
            EnumSet.of(ReservationStatus.AKTIVNA, ReservationStatus.PLACANJE_NEUSPESNO);

    private final ParkingSpotRepository spotRepository;
    private final ParkingLotService lotService;
    private final ReservationRepository reservationRepository;

    public ParkingSpotService(ParkingSpotRepository spotRepository,
                              ParkingLotService lotService,
                              ReservationRepository reservationRepository) {
        this.spotRepository = spotRepository;
        this.lotService = lotService;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Spisak mesta na lokaciji. Ako su zadati from i to, za svako mesto se racuna
     * da li je slobodno u tom intervalu (nije van upotrebe i nema preklapajucu rezervaciju).
     */
    @Transactional(readOnly = true)
    public List<SpotResponse> listForLot(Long lotId, Instant from, Instant to) {
        lotService.getEntity(lotId);
        List<ParkingSpot> spots = spotRepository.findByLotIdOrderBySpotNumberAsc(lotId);

        Set<Long> busy = null;
        if (from != null && to != null) {
            if (!to.isAfter(from)) {
                throw new BadRequestException("Parametar 'to' mora biti posle 'from'");
            }
            busy = new HashSet<>(reservationRepository.findBusySpotIds(
                    lotId, ReservationStatus.BLOCKING, from, to));
        } else if (from != null || to != null) {
            throw new BadRequestException("Za proveru dostupnosti zadajte i 'from' i 'to'");
        }

        List<SpotResponse> result = new ArrayList<>(spots.size());
        for (ParkingSpot s : spots) {
            Boolean available = null;
            if (busy != null) {
                available = s.getStatus() != SpotStatus.VAN_UPOTREBE && !busy.contains(s.getId());
            }
            result.add(toResponse(s, available));
        }
        return result;
    }

    @Transactional
    public SpotResponse create(Long lotId, String spotNumber) {
        ParkingLot lot = lotService.getEntity(lotId);
        ensureCapacity(lot, 1);
        String number = spotNumber.trim().toUpperCase();
        if (spotRepository.existsByLotIdAndSpotNumber(lotId, number)) {
            throw new ConflictException("Mesto " + number + " vec postoji na ovoj lokaciji");
        }
        return toResponse(spotRepository.save(newSpot(lot, number)), null);
    }

    /** Pravi vise mesta odjednom (A1..A10); preskace oznake koje vec postoje. */
    @Transactional
    public List<SpotResponse> createBulk(Long lotId, BulkSpotsRequest req) {
        ParkingLot lot = lotService.getEntity(lotId);
        String prefix = req.prefix() == null ? "" : req.prefix().trim().toUpperCase();

        List<ParkingSpot> toCreate = new ArrayList<>();
        int n = 1;
        while (toCreate.size() < req.count()) {
            String number = prefix + n++;
            if (!spotRepository.existsByLotIdAndSpotNumber(lotId, number)) {
                toCreate.add(newSpot(lot, number));
            }
        }
        ensureCapacity(lot, toCreate.size());
        return spotRepository.saveAll(toCreate).stream().map(s -> toResponse(s, null)).toList();
    }

    @Transactional
    public SpotResponse rename(Long spotId, String spotNumber) {
        ParkingSpot spot = getEntity(spotId);
        String number = spotNumber.trim().toUpperCase();
        if (!number.equals(spot.getSpotNumber())
                && spotRepository.existsByLotIdAndSpotNumber(spot.getLot().getId(), number)) {
            throw new ConflictException("Mesto " + number + " vec postoji na ovoj lokaciji");
        }
        spot.setSpotNumber(number);
        return toResponse(spot, null);
    }

    /**
     * FR-08: operater rucno stavlja mesto van upotrebe (uz razlog) ili ga vraca u upotrebu.
     * Ne dozvoljava se dok je na mestu parkiranje u toku.
     */
    @Transactional
    public SpotResponse changeServiceStatus(Long spotId, SpotServiceStatusRequest req) {
        ParkingSpot spot = getEntity(spotId);

        if (Boolean.TRUE.equals(req.outOfService())) {
            if (req.reason() == null || req.reason().isBlank()) {
                throw new BadRequestException("Razlog je obavezan kada se mesto stavlja van upotrebe");
            }
            if (reservationRepository.existsBySpotIdAndStatusIn(spotId, IN_PROGRESS)) {
                throw new ConflictException("Na mestu je parkiranje u toku; mesto se ne moze staviti van upotrebe");
            }
            spot.setStatus(SpotStatus.VAN_UPOTREBE);
            spot.setOutOfServiceReason(req.reason().trim());
        } else {
            if (spot.getStatus() != SpotStatus.VAN_UPOTREBE) {
                throw new ConflictException("Mesto nije van upotrebe");
            }
            // Vraca se kao slobodno; periodicni posao ce ga oznaciti kao REZERVISANO ako treba.
            spot.setStatus(SpotStatus.SLOBODNO);
            spot.setOutOfServiceReason(null);
        }
        return toResponse(spot, null);
    }

    public ParkingSpot getEntity(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new NotFoundException("Mesto " + spotId + " ne postoji"));
    }

    private void ensureCapacity(ParkingLot lot, int adding) {
        long existing = spotRepository.countByLotId(lot.getId());
        if (existing + adding > lot.getCapacity()) {
            throw new ConflictException("Prekoracen kapacitet lokacije: postoji " + existing
                    + " mesta, kapacitet je " + lot.getCapacity());
        }
    }

    private static ParkingSpot newSpot(ParkingLot lot, String number) {
        ParkingSpot s = new ParkingSpot();
        s.setLot(lot);
        s.setSpotNumber(number);
        s.setStatus(SpotStatus.SLOBODNO);
        return s;
    }

    static SpotResponse toResponse(ParkingSpot s, Boolean available) {
        return new SpotResponse(s.getId(), s.getLot().getId(), s.getSpotNumber(), s.getStatus(),
                s.getOutOfServiceReason(), available);
    }
}
