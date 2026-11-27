package com.smartpark.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** FR-11: periodicno (podrazumevano na 60 s) obradjuje istekle i predstojece rezervacije. */
@Component
public class ReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationScheduler.class);

    private final ReservationService reservationService;

    public ReservationScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${smartpark.reservation.check-interval-ms:60000}",
               initialDelayString = "${smartpark.reservation.initial-delay-ms:30000}")
    public void run() {
        int expired = reservationService.expireOverdue();
        int marked = reservationService.markUpcomingSpotsReserved();
        if (expired > 0 || marked > 0) {
            log.info("Rezervacije: isteklo {}, mesta oznacena kao rezervisana {}", expired, marked);
        }
    }
}
