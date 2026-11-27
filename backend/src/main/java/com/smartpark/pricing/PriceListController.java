package com.smartpark.pricing;

import com.smartpark.pricing.dto.PriceListRequest;
import com.smartpark.pricing.dto.PriceListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PriceListController {

    private final PriceListService service;

    public PriceListController(PriceListService service) {
        this.service = service;
    }

    /** Vazeci cenovnik - vidi ga svaki prijavljeni korisnik (vozac pre rezervacije). */
    @GetMapping("/api/lots/{lotId}/price-list")
    @PreAuthorize("isAuthenticated()")
    public PriceListResponse current(@PathVariable Long lotId) {
        return service.current(lotId);
    }

    /** Istorija verzija cenovnika - administrator. */
    @GetMapping("/api/price-lists")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<PriceListResponse> history(@RequestParam Long lotId) {
        return service.history(lotId);
    }

    /** FR-15: nova verzija cenovnika za lokaciju. */
    @PutMapping("/api/price-lists/{lotId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<PriceListResponse> createVersion(@PathVariable Long lotId,
                                                           @Valid @RequestBody PriceListRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVersion(lotId, req));
    }
}
