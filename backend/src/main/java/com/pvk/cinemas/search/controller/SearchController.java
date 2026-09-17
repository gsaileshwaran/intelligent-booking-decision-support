package com.pvk.cinemas.search.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.search.dto.SearchCardResponse;
import com.pvk.cinemas.search.dto.SearchRequest;
import com.pvk.cinemas.search.service.SearchOrchestrationService;
import com.pvk.cinemas.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchOrchestrationService searchService;

    public SearchController(SearchOrchestrationService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchCardResponse>>> searchGet(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer cityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getUserId() : null;
        // Accept both ?q= and ?query= — whichever is non-blank wins
        String effectiveQuery = (q != null && !q.isBlank()) ? q : (query != null && !query.isBlank() ? query : q);
        List<SearchCardResponse> results = searchService.search(effectiveQuery, cityId, userId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<SearchCardResponse>>> searchPost(
            @RequestBody SearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getUserId() : null;
        List<SearchCardResponse> results = searchService.search(request.getQuery(), request.getCityId(), userId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
