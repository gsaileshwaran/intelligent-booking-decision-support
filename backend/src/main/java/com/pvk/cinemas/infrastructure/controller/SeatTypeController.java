package com.pvk.cinemas.infrastructure.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.common.exceptions.ResourceNotFoundException;
import com.pvk.cinemas.infrastructure.dto.SeatTypeResponse;
import com.pvk.cinemas.infrastructure.model.SeatType;
import com.pvk.cinemas.infrastructure.repository.SeatTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/seat-types")
public class SeatTypeController {

    private final SeatTypeRepository seatTypeRepository;

    public SeatTypeController(SeatTypeRepository seatTypeRepository) {
        this.seatTypeRepository = seatTypeRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatTypeResponse>>> getAllSeatTypes() {
        List<SeatTypeResponse> types = seatTypeRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getSeatTypeId(), b.getSeatTypeId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(types));
    }

    @GetMapping("/{seatTypeId}")
    public ResponseEntity<ApiResponse<SeatTypeResponse>> getSeatTypeById(@PathVariable Long seatTypeId) {
        SeatType seatType = seatTypeRepository.findById(seatTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat type not found: " + seatTypeId));
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(seatType)));
    }

    private SeatTypeResponse mapToResponse(SeatType st) {
        return new SeatTypeResponse(st.getSeatTypeId(), st.getTypeCode(), st.getName(), st.getDescription());
    }
}
