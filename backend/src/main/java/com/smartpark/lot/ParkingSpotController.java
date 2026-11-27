package com.smartpark.lot;

import com.smartpark.lot.dto.SpotRequest;
import com.smartpark.lot.dto.SpotResponse;
import com.smartpark.lot.dto.SpotServiceStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots")
public class ParkingSpotController {

    private final ParkingSpotService spotService;

    public ParkingSpotController(ParkingSpotService spotService) {
        this.spotService = spotService;
    }

    /** FR-06: administrator menja oznaku mesta. */
    @PutMapping("/{spotId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public SpotResponse rename(@PathVariable Long spotId, @Valid @RequestBody SpotRequest req) {
        return spotService.rename(spotId, req.spotNumber());
    }

    /** FR-08: operater (i administrator) stavlja mesto van upotrebe ili ga vraca. */
    @PatchMapping("/{spotId}/service-status")
    @PreAuthorize("hasAnyRole('OPERATER','ADMINISTRATOR')")
    public SpotResponse changeServiceStatus(@PathVariable Long spotId,
                                            @Valid @RequestBody SpotServiceStatusRequest req) {
        return spotService.changeServiceStatus(spotId, req);
    }
}
