package com.smartpark.lot;

import com.smartpark.common.exception.BadRequestException;
import com.smartpark.common.exception.ConflictException;
import com.smartpark.lot.dto.BulkSpotsRequest;
import com.smartpark.lot.dto.SpotResponse;
import com.smartpark.lot.dto.SpotServiceStatusRequest;
import com.smartpark.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceTest {

    @Mock ParkingSpotRepository spotRepository;
    @Mock ParkingLotService lotService;
    @Mock ReservationRepository reservationRepository;

    ParkingSpotService service;
    ParkingLot lot;
    ParkingSpot spot;

    @BeforeEach
    void setUp() {
        service = new ParkingSpotService(spotRepository, lotService, reservationRepository);
        lot = new ParkingLot();
        lot.setId(1L);
        lot.setCapacity(5);
        lot.setActive(true);

        spot = new ParkingSpot();
        spot.setId(10L);
        spot.setLot(lot);
        spot.setSpotNumber("A1");
        spot.setStatus(SpotStatus.SLOBODNO);
    }

    @Test
    void bulkCreateRespectsCapacity() {
        when(lotService.getEntity(1L)).thenReturn(lot);
        when(spotRepository.existsByLotIdAndSpotNumber(eq(1L), anyString())).thenReturn(false);
        when(spotRepository.countByLotId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.createBulk(1L, new BulkSpotsRequest("A", 3)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("kapacitet");
        verify(spotRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreateSkipsExistingNumbers() {
        when(lotService.getEntity(1L)).thenReturn(lot);
        when(spotRepository.existsByLotIdAndSpotNumber(eq(1L), anyString()))
                .thenAnswer(inv -> "B1".equals(inv.getArgument(1)));
        when(spotRepository.countByLotId(1L)).thenReturn(1L);
        when(spotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<SpotResponse> created = service.createBulk(1L, new BulkSpotsRequest("b", 2));

        assertThat(created).extracting(SpotResponse::spotNumber).containsExactly("B2", "B3");
    }

    @Test
    void outOfServiceRequiresReason() {
        when(spotRepository.findById(10L)).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> service.changeServiceStatus(10L, new SpotServiceStatusRequest(true, "  ")))
                .isInstanceOf(BadRequestException.class);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.SLOBODNO);
    }

    @Test
    void outOfServiceRejectedWhileParkingInProgress() {
        when(spotRepository.findById(10L)).thenReturn(Optional.of(spot));
        when(reservationRepository.existsBySpotIdAndStatusIn(eq(10L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.changeServiceStatus(10L, new SpotServiceStatusRequest(true, "Kvar rampe")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void outOfServiceAndBack() {
        when(spotRepository.findById(10L)).thenReturn(Optional.of(spot));
        when(reservationRepository.existsBySpotIdAndStatusIn(eq(10L), any())).thenReturn(false);

        SpotResponse off = service.changeServiceStatus(10L, new SpotServiceStatusRequest(true, "Radovi na asfaltu"));
        assertThat(off.status()).isEqualTo(SpotStatus.VAN_UPOTREBE);
        assertThat(off.outOfServiceReason()).isEqualTo("Radovi na asfaltu");

        SpotResponse on = service.changeServiceStatus(10L, new SpotServiceStatusRequest(false, null));
        assertThat(on.status()).isEqualTo(SpotStatus.SLOBODNO);
        assertThat(on.outOfServiceReason()).isNull();
    }
}
