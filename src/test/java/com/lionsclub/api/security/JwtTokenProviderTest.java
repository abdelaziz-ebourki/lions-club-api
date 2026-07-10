package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.lionsclub.api.domain.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hs256");
        jwtConfig.setExpiration(java.time.Duration.ofMinutes(15));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    void shouldGenerateToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void shouldValidateOwnToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        DecodedJWT decoded = jwtTokenProvider.validateToken(token);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void shouldExtractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.ADMIN);
        UUID extracted = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void shouldExtractRoleFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.ADMIN);
        String role = jwtTokenProvider.getRoleFromToken(token);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectExpiredToken() {
        jwtConfig.setExpiration(java.time.Duration.ofSeconds(-1));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tampered))
                .isInstanceOf(RuntimeException.class);
    }
}
