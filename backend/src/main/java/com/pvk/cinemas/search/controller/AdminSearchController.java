package com.pvk.cinemas.search.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.search.dto.SearchIndexStatusResponse;
import com.pvk.cinemas.search.service.SearchOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/search")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSearchController {

    private final SearchOrchestrationService searchService;

    public AdminSearchController(SearchOrchestrationService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<String>> triggerReindex() {
        searchService.reindexAll();
        return ResponseEntity.accepted().body(ApiResponse.ok("Search reindexing triggered successfully", null));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SearchIndexStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(searchService.getIndexStatus()));
    }
}
