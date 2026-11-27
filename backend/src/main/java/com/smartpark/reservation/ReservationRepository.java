package com.smartpark.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    /** Koliko "zivih" rezervacija istog mesta se preklapa sa intervalom [startAt, endAt). */
    @Query("""
            select count(r) from Reservation r
            where r.spot.id = :spotId
              and r.status in :statuses
              and r.startAt < :endAt
              and r.endAt > :startAt
            """)
    long countOverlapping(@Param("spotId") Long spotId,
                          @Param("statuses") Collection<ReservationStatus> statuses,
                          @Param("startAt") Instant startAt,
                          @Param("endAt") Instant endAt);

    /** ID-jevi mesta na lokaciji koja su zauzeta rezervacijom u intervalu - za prikaz dostupnosti. */
    @Query("""
            select distinct r.spot.id from Reservation r
            where r.spot.lot.id = :lotId
              and r.status in :statuses
              and r.startAt < :endAt
              and r.endAt > :startAt
            """)
    List<Long> findBusySpotIds(@Param("lotId") Long lotId,
                               @Param("statuses") Collection<ReservationStatus> statuses,
                               @Param("startAt") Instant startAt,
                               @Param("endAt") Instant endAt);

    List<Reservation> findByUserIdOrderByStartAtDesc(Long userId);

    /** Potvrdjene rezervacije ciji je pocetak + dozvoljeni rok vec prosao (FR-11). */
    List<Reservation> findByStatusAndStartAtBefore(ReservationStatus status, Instant deadline);

    /** Potvrdjene rezervacije ciji prozor uskoro pocinje - mesto treba prikazati kao REZERVISANO. */
    @Query("""
            select r from Reservation r join fetch r.spot s
            where r.status = com.smartpark.reservation.ReservationStatus.POTVRDJENA
              and r.startAt <= :until
              and r.endAt > :now
              and s.status = com.smartpark.lot.SpotStatus.SLOBODNO
            """)
    List<Reservation> findUpcomingOnFreeSpots(@Param("now") Instant now, @Param("until") Instant until);

    boolean existsBySpotIdAndStatusIn(Long spotId, Collection<ReservationStatus> statuses);

    @Query("""
            select case when count(r) > 0 then true else false end from Reservation r
            where r.spot.lot.id = :lotId
              and r.status in :statuses
              and r.endAt > :now
            """)
    boolean existsLiveOnLot(@Param("lotId") Long lotId,
                            @Param("statuses") Collection<ReservationStatus> statuses,
                            @Param("now") Instant now);

    /** Da li jos neka rezervacija drzi mesto u trenutku "now" (osim date) - pre oslobadjanja mesta. */
    @Query("""
            select case when count(r) > 0 then true else false end from Reservation r
            where r.spot.id = :spotId
              and r.id <> :exceptId
              and r.status in :statuses
              and r.startAt <= :until
              and r.endAt > :now
            """)
    boolean existsOtherHoldingSpot(@Param("spotId") Long spotId,
                                   @Param("exceptId") Long exceptId,
                                   @Param("statuses") Collection<ReservationStatus> statuses,
                                   @Param("now") Instant now,
                                   @Param("until") Instant until);
}
