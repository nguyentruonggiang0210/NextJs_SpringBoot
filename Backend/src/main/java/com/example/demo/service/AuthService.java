package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return new LoginResponse(false, "Mật khẩu không được để trống", null, null, null);
        }

        Optional<User> optionalUser = userRepository.findById(request.getId());
        if (optionalUser.isEmpty()) {
            return new LoginResponse(false, "ID không tồn tại", null, null, null);
        }

        User user = optionalUser.get();
        String storedPassword = user.getPassword();

        if (storedPassword == null || storedPassword.isBlank()) {
            return new LoginResponse(false, "Tài khoản chưa có mật khẩu. Liên hệ admin!", null, null, null);
        }

        boolean matched;
        if (storedPassword.startsWith("$2")) {
            // Password đã được hash BCrypt — so sánh bình thường
            matched = passwordEncoder.matches(request.getPassword(), storedPassword);
        } else {
            // Password còn là plaintext (dữ liệu cũ) — so sánh trực tiếp
            matched = storedPassword.equals(request.getPassword());
            if (matched) {
                // Tự động upgrade lên BCrypt để lần sau dùng hash
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!matched) {
            return new LoginResponse(false, "Mật khẩu không đúng", null, null, null);
        }

        // Password correct — if another session exists, revoke it and let this login proceed.
        // This handles the case where the frontend expired/cleared tokens without calling /logout.
        if (refreshTokenService.hasActiveSession(user)) {
            refreshTokenService.revokeAllByUser(user);
        }

        String role = (user.getPermission() != null) ? user.getPermission().getName() : "guest";
        String accessToken = jwtService.generateToken(user.getId(), user.getName(), role);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserResponseDTO userDTO = new UserResponseDTO(
                user.getId(), user.getName(), user.getEmail(),
                user.getPhone(), null, UUID.randomUUID().toString(),
                user.getPermission() != null ? user.getPermission().getName() : null);

        return new LoginResponse(true, "Đăng nhập thành công",
                accessToken, refreshToken.getToken(), userDTO);
    }

    public LoginResponse refresh(String refreshTokenStr) {
        Optional<RefreshToken> optToken = refreshTokenService.findByToken(refreshTokenStr);

        if (optToken.isEmpty() || !refreshTokenService.isValid(optToken.get())) {
            return new LoginResponse(false, "Refresh token không hợp lệ hoặc đã hết hạn",
                    null, null, null);
        }

        RefreshToken oldToken = optToken.get();
        User user = oldToken.getUser();

        // Rotate: thu hồi token cũ, cấp token mới
        refreshTokenService.revokeToken(oldToken);
        String role = (user.getPermission() != null) ? user.getPermission().getName() : "guest";
        String newAccessToken = jwtService.generateToken(user.getId(), user.getName(), role);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        UserResponseDTO userDTO = new UserResponseDTO(
                user.getId(), user.getName(), user.getEmail(),
                user.getPhone(), null, UUID.randomUUID().toString(),
                user.getPermission() != null ? user.getPermission().getName() : null);

        return new LoginResponse(true, "Làm mới token thành công",
                newAccessToken, newRefreshToken.getToken(), userDTO);
    }

    public void logout(String refreshTokenStr) {
        refreshTokenService.findByToken(refreshTokenStr)
                .ifPresent(refreshTokenService::revokeToken);
    }
}
