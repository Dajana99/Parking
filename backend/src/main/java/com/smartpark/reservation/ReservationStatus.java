package com.smartpark.reservation;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stanja rezervacije i parkiranja (odeljak 4 specifikacije).
 * U kodu i bazi se koriste oznake bez dijakritika: POTVRDJENA, PLACANJE_NEUSPESNO, ZAVRSENA.
 */
public enum ReservationStatus {
    KREIRANA,
    POTVRDJENA,
    OTKAZANA,
    ISTEKLA,
    AKTIVNA,
    PLACANJE_NEUSPESNO,
    ZAVRSENA;

    /** Stanja u kojima rezervacija "drzi" mesto - u njima se proverava preklapanje. */
    public static final Set<ReservationStatus> BLOCKING =
            EnumSet.of(KREIRANA, POTVRDJENA, AKTIVNA, PLACANJE_NEUSPESNO);

    public boolean isFinal() {
        return this == OTKAZANA || this == ISTEKLA || this == ZAVRSENA;
    }
}
