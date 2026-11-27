package com.smartpark.reservation;

import com.smartpark.common.exception.BadRequestException;
import com.smartpark.common.exception.ConflictException;
import com.smartpark.common.exception.ForbiddenException;
import com.smartpark.common.exception.InvalidStateTransitionException;
import com.smartpark.common.exception.NotFoundException;
import com.smartpark.lot.ParkingLot;
import com.smartpark.lot.ParkingSpot;
import com.smartpark.lot.ParkingSpotRepository;
import com.smartpark.lot.SpotStatus;
import com.smartpark.pricing.PriceList;
import com.smartpark.pricing.PriceListService;
import com.smartpark.reservation.dto.CreateReservationRequest;
import com.smartpark.reservation.dto.ReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-12-01T09:00:00Z");
    private static final Long DRIVER = 7L;

    @Mock ReservationRepository reservationRepository;
    @Mock ParkingSpotRepository spotRepository;
    @Mock PriceListService priceListService;
    @Mock ApplicationEventPublisher events;

    ReservationService service;
    ParkingSpot spot;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ReservationService(reservationRepository, spotRepository, priceListService,
                events, clock, 15, 24, 30);

        ParkingLot lot = new ParkingLot();
        lot.setId(1L);
        lot.setName("Centar");
        lot.setAddress("Bulevar 1");
        lot.setCapacity(10);
        lot.setActive(true);

        spot = new ParkingSpot();
        spot.setId(5L);
        spot.setLot(lot);
        spot.setSpotNumber("A1");
        spot.setStatus(SpotStatus.SLOBODNO);
    }

    private CreateReservationRequest request(Duration startIn, Duration length) {
        Instant start = NOW.plus(startIn);
        return new CreateReservationRequest(5L, "ns 123 ab",
                start.atOffset(ZoneOffset.UTC), start.plus(length).atOffset(ZoneOffset.UTC));
    }

    private void spotFoundWithPriceList() {
        when(spotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(spot));
        when(priceListService.validAt(eq(1L), any())).thenReturn(new PriceList());
    }

    private Reservation existing(Long owner, ReservationStatus... path) {
        Reservation r = new Reservation();
        r.setId(100L);
        r.setUserId(owner);
        r.setSpot(spot);
        r.setPlateNumber("NS123AB");
        r.setStartAt(NOW.plus(Duration.ofHours(1)));
        r.setEndAt(NOW.plus(Duration.ofHours(3)));
        for (ReservationStatus s : path) {
            r.transitionTo(s);
        }
        return r;
    }

    // ---------------------------------------------------------------- create

    @Test
    void createConfirmsReservationAndMarksSpotWhenItStartsSoon() {
        spotFoundWithPriceList();
        when(reservationRepository.countOverlapping(eq(5L), any(), any(), any())).thenReturn(0L);
        when(reservationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.create(DRIVER, request(Duration.ofMinutes(5), Duration.ofHours(2)));

        assertThat(res.status()).isEqualTo(ReservationStatus.POTVRDJENA);
        assertThat(res.plateNumber()).isEqualTo("NS123AB");
        assertThat(res.userId()).isEqualTo(DRIVER);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.REZERVISANO);
    }

    @Test
    void createForLaterKeepsSpotFree() {
        spotFoundWithPriceList();
        when(reservationRepository.countOverlapping(eq(5L), any(), any(), any())).thenReturn(0L);
        when(reservationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(DRIVER, request(Duration.ofHours(5), Duration.ofHours(1)));

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.SLOBODNO);
    }

    @Test
    void createRejectsOverlappingReservation() {
        spotFoundWithPriceList();
        when(reservationRepository.countOverlapping(eq(5L), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofHours(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vec rezervisano");
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsSpotOutOfService() {
        spot.setStatus(SpotStatus.VAN_UPOTREBE);
        when(spotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofHours(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("van upotrebe");
    }

    @Test
    void createRejectsCurrentlyOccupiedSpotForNearTerm() {
        spot.setStatus(SpotStatus.ZAUZETO);
        when(spotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofMinutes(1), Duration.ofHours(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("zauzeto");
    }

    @Test
    void createRejectsInactiveLot() {
        spot.getLot().setActive(false);
        when(spotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofHours(1))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsLotWithoutPriceList() {
        when(spotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(spot));
        when(priceListService.validAt(eq(1L), any())).thenThrow(new NotFoundException("nema"));

        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofHours(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cenovnik");
    }

    @Test
    void createRejectsEndBeforeStart() {
        CreateReservationRequest bad = new CreateReservationRequest(5L, "NS123AB",
                NOW.plus(Duration.ofHours(2)).atOffset(ZoneOffset.UTC),
                NOW.plus(Duration.ofHours(1)).atOffset(ZoneOffset.UTC));

        assertThatThrownBy(() -> service.create(DRIVER, bad)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(spotRepository);
    }

    @Test
    void createRejectsTooShortTooLongPastAndTooFarAhead() {
        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofMinutes(10))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(1), Duration.ofHours(25))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofHours(-2), Duration.ofHours(1))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(DRIVER, request(Duration.ofDays(31), Duration.ofHours(1))))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void ownerCanCancelConfirmedReservationAndSpotIsFreed() {
        spot.setStatus(SpotStatus.REZERVISANO);
        Reservation r = existing(DRIVER, ReservationStatus.POTVRDJENA);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));
        when(reservationRepository.existsOtherHoldingSpot(eq(5L), eq(100L), any(), any(), any())).thenReturn(false);

        ReservationResponse res = service.cancel(100L, DRIVER);

        assertThat(res.status()).isEqualTo(ReservationStatus.OTKAZANA);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.SLOBODNO);
    }

    @Test
    void cannotCancelSomeoneElsesReservation() {
        Reservation r = existing(99L, ReservationStatus.POTVRDJENA);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(100L, DRIVER)).isInstanceOf(ForbiddenException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.POTVRDJENA);
    }

    @Test
    void cannotCancelAfterParkingStarted() {
        Reservation r = existing(DRIVER, ReservationStatus.POTVRDJENA, ReservationStatus.AKTIVNA);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(100L, DRIVER))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    // ---------------------------------------------------------------- expiry

    @Test
    void overdueReservationExpiresFreesSpotAndPublishesEvent() {
        spot.setStatus(SpotStatus.REZERVISANO);
        Reservation r = existing(DRIVER, ReservationStatus.POTVRDJENA);
        when(reservationRepository.findByStatusAndStartAtBefore(ReservationStatus.POTVRDJENA,
                NOW.minus(Duration.ofMinutes(15)))).thenReturn(List.of(r));
        when(reservationRepository.existsOtherHoldingSpot(anyLong(), anyLong(), any(), any(), any())).thenReturn(false);

        int expired = service.expireOverdue();

        assertThat(expired).isEqualTo(1);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.ISTEKLA);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.SLOBODNO);
        verify(events).publishEvent(new ReservationExpiredEvent(100L, DRIVER, 1L));
    }

    // ---------------------------------------------------------------- changeStatus

    @Test
    void parkingCannotStartOutsideReservationWindow() {
        Reservation r = existing(DRIVER, ReservationStatus.POTVRDJENA);
        r.setStartAt(NOW.plus(Duration.ofHours(2)));
        r.setEndAt(NOW.plus(Duration.ofHours(3)));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.changeStatus(100L, ReservationStatus.AKTIVNA))
                .isInstanceOf(ConflictException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.POTVRDJENA);
    }

    @Test
    void startingParkingMarksSpotOccupied() {
        Reservation r = existing(DRIVER, ReservationStatus.POTVRDJENA);
        r.setStartAt(NOW.minus(Duration.ofMinutes(5)));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(r));

        service.changeStatus(100L, ReservationStatus.AKTIVNA);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.AKTIVNA);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.ZAUZETO);
    }

    @Test
    void plateIsNormalized() {
        assertThat(ReservationService.normalizePlate("  bg 123 cd ")).isEqualTo("BG123CD");
    }
}
