package com.cinema.ticketing.controller;

import com.cinema.ticketing.dto.concert.AvailabilityResponse;
import com.cinema.ticketing.dto.concert.ConcertCategoryUpsertRequest;
import com.cinema.ticketing.dto.concert.ConcertResponse;
import com.cinema.ticketing.dto.concert.ConcertUpsertRequest;
import com.cinema.ticketing.dto.concert.PricingResponse;
import com.cinema.ticketing.dto.concert.SeedConcertRequest;
import com.cinema.ticketing.service.ConcertService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/concerts")
public class ConcertController {

    private final ConcertService concertService;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public List<ConcertResponse> list(@RequestParam(required = false) String name,
                                      @RequestParam(required = false) String artist,
                                      @RequestParam(required = false) String venue) {
        return concertService.list(name, artist, venue);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public ConcertResponse getById(@PathVariable UUID id) {
        return concertService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, UUID> create(@Valid @RequestBody ConcertUpsertRequest request) {
        return Map.of("id", concertService.create(request));
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, UUID> seed(@Valid @RequestBody SeedConcertRequest request) {
        return Map.of("id", concertService.seedConcert(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> update(@PathVariable UUID id, @Valid @RequestBody ConcertUpsertRequest request) {
        concertService.update(id, request);
        return Map.of("message", "Concert updated");
    }

    @PostMapping("/{id}/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> upsertCategories(@PathVariable UUID id,
                                                @Valid @RequestBody List<ConcertCategoryUpsertRequest> requests) {
        concertService.upsertCategories(id, requests);
        return Map.of("message", "Categories upserted");
    }

    @GetMapping("/{id}/pricing")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public List<PricingResponse> pricing(@PathVariable UUID id) {
        return concertService.pricing(id);
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEWER')")
    public List<AvailabilityResponse> availability(@PathVariable UUID id) {
        return concertService.availability(id);
    }
}
