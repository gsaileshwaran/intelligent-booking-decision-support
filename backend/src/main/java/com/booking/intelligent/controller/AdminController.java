package com.booking.intelligent.controller;

import com.booking.intelligent.dto.AdminBranchStatsDto;
import com.booking.intelligent.dto.AdminDashboardStatsDto;
import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.BookingResponse;
import com.booking.intelligent.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getDashboardStats() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard metrics retrieved successfully", stats));
    }

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<AdminBranchStatsDto>>> getBranchStats() {
        List<AdminBranchStatsDto> branchStats = adminService.getBranchStats();
        return ResponseEntity.ok(ApiResponse.success("PVK branches statistics retrieved successfully", branchStats));
    }

    @GetMapping("/recent-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getRecentBookings(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<BookingResponse> recent = adminService.getRecentBookings(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent system bookings retrieved successfully", recent));
    }
}
