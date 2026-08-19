package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.RecommendationRequestDto;
import com.booking.intelligent.dto.RecommendationResponseDto;
import com.booking.intelligent.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<RecommendationResponseDto>> evaluateRecommendations(
            @RequestBody RecommendationRequestDto request) {
        RecommendationResponseDto response = recommendationService.evaluateRecommendations(request);
        return ResponseEntity.ok(ApiResponse.success("Recommendations evaluated successfully", response));
    }
}
