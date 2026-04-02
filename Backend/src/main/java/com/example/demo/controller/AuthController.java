package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** POST /api/auth/refresh  — body: { "refreshToken": "..." } */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "refreshToken là bắt buộc", null, null, null));
        }
        LoginResponse response = authService.refresh(refreshToken);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(401).body(response);
    }

    /** POST /api/auth/logout  — body: { "refreshToken": "..." } */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent().build();
    }
}