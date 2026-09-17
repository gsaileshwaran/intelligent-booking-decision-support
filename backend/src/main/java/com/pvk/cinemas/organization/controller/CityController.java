package com.pvk.cinemas.organization.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.organization.dto.CityResponse;
import com.pvk.cinemas.organization.dto.TheatreResponse;
import com.pvk.cinemas.organization.service.CityService;
import com.pvk.cinemas.organization.service.TheatreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;
    private final TheatreService theatreService;

    public CityController(CityService cityService, TheatreService theatreService) {
        this.cityService = cityService;
        this.theatreService = theatreService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities() {
        return ResponseEntity.ok(ApiResponse.ok(cityService.getAllCities()));
    }

    @GetMapping("/{cityId}/theatres")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getTheatresByCity(@PathVariable Integer cityId) {
        return ResponseEntity.ok(ApiResponse.ok(theatreService.getTheatresByCity(cityId)));
    }
}
