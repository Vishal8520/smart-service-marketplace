package com.example.marketplace.controller;

import com.example.marketplace.dto.response.CityResponse;
import com.example.marketplace.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@Tag(name = "Cities", description = "Public city listing endpoint for location-based service filtering")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    @Operation(summary = "Get list of active cities for service filtering")
    public ResponseEntity<List<CityResponse>> getActiveCities() {
        return ResponseEntity.ok(cityService.getActiveCities());
    }
}
