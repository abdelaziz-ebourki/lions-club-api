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
    void shouldGenerateTokenWithAllClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void shouldValidateOwnToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        DecodedJWT decoded = jwtTokenProvider.validateToken(token);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
    }

    @Test
    void shouldExtractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.ADMIN, "John", "Doe");
        UUID extracted = UUID.fromString(jwtTokenProvider.validateToken(token).getSubject());
        assertThat(extracted).isEqualTo(userId);
    }

    @Test
    void shouldExtractRoleFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.ADMIN, "John", "Doe");
        String role = jwtTokenProvider.validateToken(token).getClaim("role").asString();
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void shouldExtractEmailFromToken() {
        UUID userId = UUID.randomUUID();
        String email = "john.doe@lionsclub.org";
        String token = jwtTokenProvider.createToken(userId, email, Role.MEMBER, "John", "Doe");
        String extractedEmail = jwtTokenProvider.validateToken(token).getClaim("email").asString();
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    void shouldExtractFirstNameFromToken() {
        UUID userId = UUID.randomUUID();
        String firstName = "John";
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, firstName, "Doe");
        String extractedFirstName = jwtTokenProvider.validateToken(token).getClaim("firstName").asString();
        assertThat(extractedFirstName).isEqualTo(firstName);
    }

    @Test
    void shouldExtractLastNameFromToken() {
        UUID userId = UUID.randomUUID();
        String lastName = "Doe";
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", lastName);
        String extractedLastName = jwtTokenProvider.validateToken(token).getClaim("lastName").asString();
        assertThat(extractedLastName).isEqualTo(lastName);
    }

    @Test
    void shouldRejectExpiredToken() {
        jwtConfig.setExpiration(java.time.Duration.ofSeconds(-1));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tampered))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldExtractAllClaimsFromValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "jane.doe@lionsclub.org";
        Role role = Role.ADMIN;
        String firstName = "Jane";
        String lastName = "Doe";

        String token = jwtTokenProvider.createToken(userId, email, role, firstName, lastName);
        DecodedJWT decoded = jwtTokenProvider.validateToken(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("ADMIN");
        assertThat(decoded.getClaim("email").asString()).isEqualTo(email);
        assertThat(decoded.getClaim("firstName").asString()).isEqualTo(firstName);
        assertThat(decoded.getClaim("lastName").asString()).isEqualTo(lastName);
    }

    @Test
    void tokenSizeIncreaseShouldBeLessThan200Bytes() {
        UUID userId = UUID.randomUUID();
        String tokenWithMinimal = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        String tokenWithAll = jwtTokenProvider.createToken(userId, "john.doe@example.com", Role.MEMBER, "John", "Doe");

        int increase = tokenWithAll.length() - tokenWithMinimal.length();
        assertThat(increase).isLessThan(200);
    }
}