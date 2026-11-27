package com.smartpark.reservation;

import com.smartpark.common.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.smartpark.reservation.ReservationStatus.*;

/**
 * FR-12: jedino mesto gde su definisani dozvoljeni prelazi stanja (tabela iz odeljka 4).
 * Svaki drugi prelaz baca InvalidStateTransitionException -> HTTP 409.
 *
 * Modul za parking sesije i placanje (FR-13, FR-14, FR-18) treba da menja stanje
 * iskljucivo kroz ReservationService.changeStatus(...), koji poziva ovu klasu.
 */
public final class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED =
            new EnumMap<>(ReservationStatus.class);

    static {
        ALLOWED.put(KREIRANA, EnumSet.of(POTVRDJENA, OTKAZANA));
        ALLOWED.put(POTVRDJENA, EnumSet.of(OTKAZANA, ISTEKLA, AKTIVNA));
        ALLOWED.put(AKTIVNA, EnumSet.of(ZAVRSENA, PLACANJE_NEUSPESNO));
        ALLOWED.put(PLACANJE_NEUSPESNO, EnumSet.of(ZAVRSENA));
        ALLOWED.put(OTKAZANA, EnumSet.noneOf(ReservationStatus.class));
        ALLOWED.put(ISTEKLA, EnumSet.noneOf(ReservationStatus.class));
        ALLOWED.put(ZAVRSENA, EnumSet.noneOf(ReservationStatus.class));
    }

    private ReservationStateMachine() {
    }

    public static boolean canTransition(ReservationStatus from, ReservationStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransition(ReservationStatus from, ReservationStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
