package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.RecommendationRequestDto;
import com.booking.intelligent.dto.RecommendationResponseDto;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<RecommendationResponseDto>> evaluateRecommendations(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody RecommendationRequestDto request) {
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        RecommendationResponseDto response = recommendationService.evaluateRecommendations(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Recommendations evaluated successfully", response));
    }
}
