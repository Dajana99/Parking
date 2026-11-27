package com.smartpark.reservation;

import com.smartpark.common.exception.BadRequestException;
import com.smartpark.common.exception.ConflictException;
import com.smartpark.common.exception.ForbiddenException;
import com.smartpark.common.exception.NotFoundException;
import com.smartpark.lot.ParkingSpot;
import com.smartpark.lot.ParkingSpotRepository;
import com.smartpark.lot.SpotStatus;
import com.smartpark.pricing.PriceListService;
import com.smartpark.reservation.dto.CreateReservationRequest;
import com.smartpark.reservation.dto.ReservationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Rezervacije: FR-09 (rezervacija bez preklapanja), FR-10 (otkazivanje),
 * FR-11 (isticanje), FR-12 (validacija prelaza stanja).
 */
@Service
public class ReservationService {

    static final Duration MIN_DURATION = Duration.ofMinutes(15);

    private final ReservationRepository reservationRepository;
    private final ParkingSpotRepository spotRepository;
    private final PriceListService priceListService;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration grace;
    private final Duration maxDuration;
    private final Duration maxAhead;

    public ReservationService(ReservationRepository reservationRepository,
                              ParkingSpotRepository spotRepository,
                              PriceListService priceListService,
                              ApplicationEventPublisher events,
                              Clock clock,
                              @Value("${smartpark.reservation.grace-minutes:15}") long graceMinutes,
                              @Value("${smartpark.reservation.max-duration-hours:24}") long maxDurationHours,
                              @Value("${smartpark.reservation.max-days-ahead:30}") long maxDaysAhead) {
        this.reservationRepository = reservationRepository;
        this.spotRepository = spotRepository;
        this.priceListService = priceListService;
        this.events = events;
        this.clock = clock;
        this.grace = Duration.ofMinutes(graceMinutes);
        this.maxDuration = Duration.ofHours(maxDurationHours);
        this.maxAhead = Duration.ofDays(maxDaysAhead);
    }

    // ------------------------------------------------------------------ FR-09

    @Transactional
    public ReservationResponse create(Long userId, CreateReservationRequest req) {
        Instant now = clock.instant();
        Instant start = req.startAt().toInstant();
        Instant end = req.endAt().toInstant();
        validateInterval(now, start, end);

        // Zakljucava mesto do kraja transakcije -> nema "trke" dva zahteva za isto mesto.
        ParkingSpot spot = spotRepository.findByIdForUpdate(req.spotId())
                .orElseThrow(() -> new NotFoundException("Mesto " + req.spotId() + " ne postoji"));

        if (!spot.getLot().isActive()) {
            throw new ConflictException("Lokacija nije aktivna");
        }
        if (spot.getStatus() == SpotStatus.VAN_UPOTREBE) {
            throw new ConflictException("Mesto je van upotrebe");
        }
        // Zauzeto mesto ne moze da se rezervise za skoriji termin (FR-32 kasnije dolazi iz IoT-a).
        if (spot.getStatus() == SpotStatus.ZAUZETO && start.isBefore(now.plus(grace))) {
            throw new ConflictException("Mesto je trenutno zauzeto");
        }
        ensurePriceListExists(spot.getLot().getId(), start);

        long overlapping = reservationRepository.countOverlapping(
                spot.getId(), ReservationStatus.BLOCKING, start, end);
        if (overlapping > 0) {
            throw new ConflictException("Mesto je vec rezervisano u trazenom intervalu");
        }

        Reservation r = new Reservation();          // stanje KREIRANA
        r.setUserId(userId);
        r.setSpot(spot);
        r.setPlateNumber(normalizePlate(req.plateNumber()));
        r.setStartAt(start);
        r.setEndAt(end);
        r.transitionTo(ReservationStatus.POTVRDJENA); // sve provere su prosle: KREIRANA -> POTVRDJENA

        try {
            reservationRepository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            // Rezervna zastita: EXCLUDE ogranicenje u bazi (reservations_no_overlap)
            throw new ConflictException("Mesto je vec rezervisano u trazenom intervalu");
        }

        if (spot.getStatus() == SpotStatus.SLOBODNO && !start.isAfter(now.plus(grace))) {
            spot.setStatus(SpotStatus.REZERVISANO);
        }
        return ReservationResponse.from(r);
    }

    // ------------------------------------------------------------------ FR-10

    @Transactional
    public ReservationResponse cancel(Long reservationId, Long userId) {
        Reservation r = getEntity(reservationId);
        if (!userId.equals(r.getUserId())) {
            throw new ForbiddenException("Mozete otkazati samo sopstvenu rezervaciju");
        }
        r.transitionTo(ReservationStatus.OTKAZANA); // AKTIVNA/ZAVRSENA -> 409
        syncSpot(r);
        return ReservationResponse.from(r);
    }

    // ------------------------------------------------------------------ FR-11

    /** Poziva ga ReservationScheduler. Vraca broj isteklih rezervacija. */
    @Transactional
    public int expireOverdue() {
        Instant deadline = clock.instant().minus(grace);
        List<Reservation> overdue = reservationRepository
                .findByStatusAndStartAtBefore(ReservationStatus.POTVRDJENA, deadline);
        for (Reservation r : overdue) {
            r.transitionTo(ReservationStatus.ISTEKLA);
            syncSpot(r);
            events.publishEvent(new ReservationExpiredEvent(
                    r.getId(), r.getUserId(), r.getSpot().getLot().getId()));
        }
        return overdue.size();
    }

    /** Mesto cija rezervacija pocinje u narednih "grace" minuta prikazuje se kao REZERVISANO. */
    @Transactional
    public int markUpcomingSpotsReserved() {
        Instant now = clock.instant();
        List<Reservation> upcoming = reservationRepository.findUpcomingOnFreeSpots(now, now.plus(grace));
        for (Reservation r : upcoming) {
            r.getSpot().setStatus(SpotStatus.REZERVISANO);
        }
        return upcoming.size();
    }

    // ------------------------------------------------------------------ FR-12 (za druge module)

    /**
     * Jedina tacka kroz koju drugi moduli (parking sesije, placanje) menjaju stanje rezervacije.
     * Npr. pocetak parkiranja: changeStatus(id, AKTIVNA); uspesno placanje: changeStatus(id, ZAVRSENA).
     * Nedozvoljen prelaz -> InvalidStateTransitionException (409).
     */
    @Transactional
    public Reservation changeStatus(Long reservationId, ReservationStatus target) {
        Reservation r = getEntity(reservationId);
        if (target == ReservationStatus.AKTIVNA) {
            Instant now = clock.instant();
            if (now.isBefore(r.getStartAt().minus(grace)) || !now.isBefore(r.getEndAt())) {
                throw new ConflictException("Parkiranje moze da pocne samo u vremenskom prozoru rezervacije");
            }
        }
        r.transitionTo(target);
        syncSpot(r);
        return r;
    }

    // ------------------------------------------------------------------ pregled

    @Transactional(readOnly = true)
    public List<ReservationResponse> myReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByStartAtDesc(userId).stream()
                .map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getForUser(Long reservationId, Long userId, boolean staff) {
        Reservation r = getEntity(reservationId);
        if (!staff && !userId.equals(r.getUserId())) {
            throw new ForbiddenException("Nemate pristup ovoj rezervaciji");
        }
        return ReservationResponse.from(r);
    }

    /** FR-25: operater i administrator - sve rezervacije uz filtere (svi su opcioni). */
    @Transactional(readOnly = true)
    public List<ReservationResponse> search(Long lotId, ReservationStatus status, Instant from, Instant to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Parametar 'to' mora biti posle 'from'");
        }
        Specification<Reservation> spec = (root, query, cb) -> cb.conjunction();
        if (lotId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("spot").get("lot").get("id"), lotId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("startAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.<Instant>get("startAt"), to));
        }
        return reservationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startAt")).stream()
                .map(ReservationResponse::from).toList();
    }

    // ------------------------------------------------------------------ pomocne

    Reservation getEntity(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Rezervacija " + reservationId + " ne postoji"));
    }

    private void validateInterval(Instant now, Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("Kraj rezervacije mora biti posle pocetka");
        }
        if (start.isBefore(now.minus(Duration.ofMinutes(1)))) {
            throw new BadRequestException("Pocetak rezervacije ne moze biti u proslosti");
        }
        Duration length = Duration.between(start, end);
        if (length.compareTo(MIN_DURATION) < 0) {
            throw new BadRequestException("Rezervacija mora trajati najmanje " + MIN_DURATION.toMinutes() + " minuta");
        }
        if (length.compareTo(maxDuration) > 0) {
            throw new BadRequestException("Rezervacija moze trajati najvise " + maxDuration.toHours() + " sati");
        }
        if (start.isAfter(now.plus(maxAhead))) {
            throw new BadRequestException("Rezervacija je moguca najvise " + maxAhead.toDays() + " dana unapred");
        }
    }

    private void ensurePriceListExists(Long lotId, Instant at) {
        try {
            priceListService.validAt(lotId, at);
        } catch (NotFoundException e) {
            throw new ConflictException("Za ovu lokaciju jos nije definisan cenovnik");
        }
    }

    /** Uskladjuje status mesta sa novim stanjem rezervacije. */
    private void syncSpot(Reservation r) {
        ParkingSpot spot = r.getSpot();
        if (spot.getStatus() == SpotStatus.VAN_UPOTREBE) {
            return;
        }
        if (r.getStatus() == ReservationStatus.AKTIVNA) {
            spot.setStatus(SpotStatus.ZAUZETO);
            return;
        }
        if (r.getStatus().isFinal()) {
            Instant now = clock.instant();
            boolean heldByOther = reservationRepository.existsOtherHoldingSpot(
                    spot.getId(), r.getId(), EnumSet.of(ReservationStatus.POTVRDJENA), now, now.plus(grace));
            // ZAUZETO posle zavrsetka sesije menjace IoT senzor u F3; do tada ga oslobadjamo ovde.
            spot.setStatus(heldByOther ? SpotStatus.REZERVISANO : SpotStatus.SLOBODNO);
        }
    }

    static String normalizePlate(String plate) {
        return plate.trim().toUpperCase().replaceAll("\\s+", "");
    }
}
