package com.smartpark.lot;

import com.smartpark.common.security.CurrentUserService;
import com.smartpark.lot.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class ParkingLotController {

    private final ParkingLotService lotService;
    private final ParkingSpotService spotService;
    private final CurrentUserService currentUser;

    public ParkingLotController(ParkingLotService lotService,
                                ParkingSpotService spotService,
                                CurrentUserService currentUser) {
        this.lotService = lotService;
        this.spotService = spotService;
        this.currentUser = currentUser;
    }

    /** FR-07: svi prijavljeni. includeInactive=true vazi samo za administratora. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LotResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return lotService.list(includeInactive && currentUser.isAdmin());
    }

    @GetMapping("/{lotId}")
    @PreAuthorize("isAuthenticated()")
    public LotResponse get(@PathVariable Long lotId) {
        return lotService.get(lotId);
    }

    /**
     * FR-07: mesta na lokaciji. Primer provere dostupnosti:
     * GET /api/lots/1/spots?from=2026-12-01T10:00:00%2B01:00&to=2026-12-01T12:00:00%2B01:00
     */
    @GetMapping("/{lotId}/spots")
    @PreAuthorize("isAuthenticated()")
    public List<SpotResponse> spots(
            @PathVariable Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return spotService.listForLot(lotId,
                from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant());
    }

    // ---------- FR-06: administrator ----------

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<LotResponse> create(@Valid @RequestBody LotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lotService.create(req));
    }

    @PutMapping("/{lotId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public LotResponse update(@PathVariable Long lotId, @Valid @RequestBody LotRequest req) {
        return lotService.update(lotId, req);
    }

    @PostMapping("/{lotId}/deactivate")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivate(@PathVariable Long lotId) {
        lotService.deactivate(lotId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{lotId}/activate")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> activate(@PathVariable Long lotId) {
        lotService.activate(lotId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{lotId}/spots")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SpotResponse> createSpot(@PathVariable Long lotId, @Valid @RequestBody SpotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.create(lotId, req.spotNumber()));
    }

    @PostMapping("/{lotId}/spots/bulk")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<SpotResponse>> createSpots(@PathVariable Long lotId,
                                                          @Valid @RequestBody BulkSpotsRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spotService.createBulk(lotId, req));
    }
}
