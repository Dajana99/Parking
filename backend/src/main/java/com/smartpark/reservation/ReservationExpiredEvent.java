package com.smartpark.reservation;

/**
 * Objavljuje se kad rezervacija istekne (vozac nije stigao u roku).
 * Modul za kazne (FR-23) treba da slusa ovaj dogadjaj i izda kaznu NEISKORISCENA_REZERVACIJA:
 *
 *   @TransactionalEventListener
 *   public void onExpired(ReservationExpiredEvent e) { penaltyService.issueNoShow(e.reservationId()); }
 */
public record ReservationExpiredEvent(Long reservationId, Long userId, Long lotId) {
}
