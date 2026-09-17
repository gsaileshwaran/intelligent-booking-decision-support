package com.pvk.cinemas.auth.controller;

import com.pvk.cinemas.auth.dto.AuthResponse;
import com.pvk.cinemas.auth.dto.CurrentUserResponse;
import com.pvk.cinemas.auth.dto.LoginRequest;
import com.pvk.cinemas.auth.dto.RegisterRequest;
import com.pvk.cinemas.auth.service.AuthService;
import com.pvk.cinemas.common.ApiResponse;
import com.pvk.cinemas.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Session terminated and token revoked successfully."
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        CurrentUserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
