package com.smartpark.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    /** Vazeca verzija u trenutku "at": poslednja ciji je valid_from <= at. */
    Optional<PriceList> findFirstByLotIdAndValidFromLessThanEqualOrderByValidFromDesc(Long lotId, Instant at);

    List<PriceList> findByLotIdOrderByValidFromDesc(Long lotId);

    boolean existsByLotIdAndValidFrom(Long lotId, Instant validFrom);
}
