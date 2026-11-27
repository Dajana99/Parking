package com.smartpark.reservation;

import com.smartpark.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStateMachineTest {

    @ParameterizedTest(name = "{0} -> {1} je dozvoljen")
    @CsvSource({
            "KREIRANA, POTVRDJENA",
            "KREIRANA, OTKAZANA",
            "POTVRDJENA, OTKAZANA",
            "POTVRDJENA, ISTEKLA",
            "POTVRDJENA, AKTIVNA",
            "AKTIVNA, ZAVRSENA",
            "AKTIVNA, PLACANJE_NEUSPESNO",
            "PLACANJE_NEUSPESNO, ZAVRSENA"
    })
    void allowedTransitions(ReservationStatus from, ReservationStatus to) {
        assertThat(ReservationStateMachine.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} je zabranjen")
    @CsvSource({
            "KREIRANA, AKTIVNA",
            "KREIRANA, ZAVRSENA",
            "POTVRDJENA, ZAVRSENA",
            "AKTIVNA, OTKAZANA",
            "ZAVRSENA, AKTIVNA",
            "OTKAZANA, POTVRDJENA",
            "ISTEKLA, AKTIVNA",
            "PLACANJE_NEUSPESNO, OTKAZANA"
    })
    void forbiddenTransitions(ReservationStatus from, ReservationStatus to) {
        assertThat(ReservationStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> ReservationStateMachine.assertTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    @Test
    void finalStatesHaveNoExits() {
        for (ReservationStatus to : ReservationStatus.values()) {
            assertThat(ReservationStateMachine.canTransition(ReservationStatus.ZAVRSENA, to)).isFalse();
            assertThat(ReservationStateMachine.canTransition(ReservationStatus.OTKAZANA, to)).isFalse();
            assertThat(ReservationStateMachine.canTransition(ReservationStatus.ISTEKLA, to)).isFalse();
        }
    }
}
