package com.smartpark.pricing;

import com.smartpark.common.exception.BadRequestException;
import com.smartpark.common.exception.ConflictException;
import com.smartpark.common.exception.NotFoundException;
import com.smartpark.lot.ParkingLot;
import com.smartpark.lot.ParkingLotService;
import com.smartpark.pricing.dto.PriceListRequest;
import com.smartpark.pricing.dto.PriceListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PriceListService {

    private final PriceListRepository repository;
    private final ParkingLotService lotService;
    private final Clock clock;

    public PriceListService(PriceListRepository repository, ParkingLotService lotService, Clock clock) {
        this.repository = repository;
        this.lotService = lotService;
        this.clock = clock;
    }

    /**
     * Cenovnik koji vazi u datom trenutku. Ovu metodu koristi obracun cene (FR-16) i kazne (FR-23):
     * npr. priceListService.validAt(lotId, session.getStartedAt()).
     */
    @Transactional(readOnly = true)
    public PriceList validAt(Long lotId, Instant at) {
        return repository.findFirstByLotIdAndValidFromLessThanEqualOrderByValidFromDesc(lotId, at)
                .orElseThrow(() -> new NotFoundException("Lokacija " + lotId + " nema vazeci cenovnik"));
    }

    @Transactional(readOnly = true)
    public PriceListResponse current(Long lotId) {
        lotService.getEntity(lotId);
        return PriceListResponse.from(validAt(lotId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<PriceListResponse> history(Long lotId) {
        lotService.getEntity(lotId);
        return repository.findByLotIdOrderByValidFromDesc(lotId).stream().map(PriceListResponse::from).toList();
    }

    /** FR-15: nova verzija cenovnika. Ne dozvoljava izmenu cena unazad. */
    @Transactional
    public PriceListResponse createVersion(Long lotId, PriceListRequest req) {
        ParkingLot lot = lotService.getEntity(lotId);
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = req.validFrom() == null
                ? now
                : req.validFrom().toInstant().truncatedTo(ChronoUnit.SECONDS);

        if (validFrom.isBefore(now)) {
            throw new BadRequestException("Cenovnik ne moze da vazi unazad (validFrom je u proslosti)");
        }
        if (repository.existsByLotIdAndValidFrom(lotId, validFrom)) {
            throw new ConflictException("Vec postoji cenovnik koji vazi od " + validFrom);
        }

        PriceList p = new PriceList();
        p.setLot(lot);
        p.setPricePerHour(req.pricePerHour());
        p.setMinCharge(req.minCharge());
        p.setOverstayFine(req.overstayFine());
        p.setNoShowFine(req.noShowFine());
        p.setValidFrom(validFrom);
        return PriceListResponse.from(repository.save(p));
    }
}
