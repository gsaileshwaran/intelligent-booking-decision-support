package com.pvk.cinemas.scheduling.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.scheduling.service.YearLongShowGenerationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/scheduling")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSchedulingController {

    private final YearLongShowGenerationService yearLongShowGenerationService;
    private final JdbcTemplate jdbcTemplate;

    public AdminSchedulingController(
            YearLongShowGenerationService yearLongShowGenerationService,
            JdbcTemplate jdbcTemplate) {
        this.yearLongShowGenerationService = yearLongShowGenerationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<YearLongShowGenerationService.GenerationSummary>> generateSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        YearLongShowGenerationService.GenerationSummary summary;
        if (startDate != null && endDate != null) {
            summary = yearLongShowGenerationService.generateScheduleRange(startDate, endDate, startDate);
        } else if (startDate != null) {
            summary = yearLongShowGenerationService.generateScheduleForDate(startDate);
        } else {
            summary = yearLongShowGenerationService.generateFullYearSchedule();
        }

        return ResponseEntity.ok(ApiResponse.ok("Schedule generated successfully", summary));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScheduleStats() {
        Long totalShows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `SHOW`", Long.class);
        Long completedShows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `SHOW` WHERE status = 'COMPLETED'", Long.class);
        Long scheduledShows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `SHOW` WHERE status = 'SCHEDULED'", Long.class);
        Long totalShowSeats = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SHOW_SEAT", Long.class);
        Map<String, Object> minMax = jdbcTemplate.queryForMap("SELECT MIN(show_date) as min_date, MAX(show_date) as max_date FROM `SHOW`");

        Map<String, Object> stats = Map.of(
                "totalShows", totalShows != null ? totalShows : 0,
                "completedShows", completedShows != null ? completedShows : 0,
                "scheduledShows", scheduledShows != null ? scheduledShows : 0,
                "totalShowSeats", totalShowSeats != null ? totalShowSeats : 0,
                "minShowDate", minMax.get("min_date") != null ? minMax.get("min_date").toString() : "",
                "maxShowDate", minMax.get("max_date") != null ? minMax.get("max_date").toString() : ""
        );

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
