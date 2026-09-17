package com.pvk.cinemas.user.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.user.dto.ProfileResponse;
import com.pvk.cinemas.user.dto.UpdateProfileRequest;
import com.pvk.cinemas.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/profile")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse profile = userService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        ProfileResponse updated = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }
}
