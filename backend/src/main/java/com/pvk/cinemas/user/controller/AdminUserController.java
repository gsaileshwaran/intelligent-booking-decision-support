package com.pvk.cinemas.user.controller;

import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import com.pvk.cinemas.user.dto.AdminUserResponse;
import com.pvk.cinemas.user.dto.CreateUserRequest;
import com.pvk.cinemas.user.dto.UpdateUserStatusRequest;
import com.pvk.cinemas.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserResponse> users = userService.getUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        AdminUserResponse created = userService.createUser(request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User provisioned successfully", created));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserPost(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        AdminUserResponse updated = userService.updateUser(userId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserPatch(
            @PathVariable Long userId,
            @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpServletRequest) {
        AdminUserResponse updated = userService.updateUser(userId, request, principal.getUserId(), httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }
}
