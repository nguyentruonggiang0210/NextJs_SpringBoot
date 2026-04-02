package com.example.demo.service;

import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    /** Tạo refresh token mới cho user (xóa token cũ nếu có). */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(
                (long) jwtProperties.getRefreshExpirationDays() * 24 * 3600));
        token.setRevoked(false);
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isValid(RefreshToken token) {
        return !token.isRevoked() && token.getExpiryDate().isAfter(Instant.now());
    }

    /**
     * Checks whether the given user already has an active (non-revoked, non-expired) refresh token,
     * which means the user is currently logged in from another session.
     *
     * @param user the user to check
     * @return true if an active session exists, false otherwise
     */
    public boolean hasActiveSession(User user) {
        return refreshTokenRepository.existsByUserAndRevokedFalseAndExpiryDateAfter(user, Instant.now());
    }

    /**
     * Revokes all refresh tokens belonging to the given user.
     * Used when the user logs in again after the frontend dropped the session without calling /logout.
     *
     * @param user the user whose tokens should be revoked
     */
    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
