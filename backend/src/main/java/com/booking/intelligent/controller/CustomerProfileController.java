package com.booking.intelligent.controller;

import com.booking.intelligent.dto.ApiResponse;
import com.booking.intelligent.dto.ChangePasswordRequestDto;
import com.booking.intelligent.dto.UpdateProfileRequestDto;
import com.booking.intelligent.dto.UserProfileDto;
import com.booking.intelligent.security.UserPrincipal;
import com.booking.intelligent.service.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerProfileController {

    @Autowired
    private CustomerProfileService customerProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileDto profile = customerProfileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileDto profile = customerProfileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<UserProfileDto>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileDto profile = customerProfileService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", profile));
    }
}
