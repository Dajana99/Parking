package com.smartpark.reservation;

import com.smartpark.common.security.CurrentUserService;
import com.smartpark.reservation.dto.CreateReservationRequest;
import com.smartpark.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final CurrentUserService currentUser;

    public ReservationController(ReservationService reservationService, CurrentUserService currentUser) {
        this.reservationService = reservationService;
        this.currentUser = currentUser;
    }

    /** FR-09 */
    @PostMapping
    @PreAuthorize("hasRole('VOZAC')")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest req) {
        ReservationResponse body = reservationService.create(currentUser.currentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** FR-10 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('VOZAC')")
    public ReservationResponse cancel(@PathVariable Long id) {
        return reservationService.cancel(id, currentUser.currentUserId());
    }

    /** Vozac vidi samo svoje rezervacije. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('VOZAC')")
    public List<ReservationResponse> my() {
        return reservationService.myReservations(currentUser.currentUserId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse get(@PathVariable Long id) {
        return reservationService.getForUser(id, currentUser.currentUserId(), currentUser.isStaff());
    }

    /** FR-25: GET /api/reservations?lotId=1&status=POTVRDJENA&from=...&to=... */
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATER','ADMINISTRATOR')")
    public List<ReservationResponse> search(
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return reservationService.search(lotId, status,
                from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant());
    }
}
