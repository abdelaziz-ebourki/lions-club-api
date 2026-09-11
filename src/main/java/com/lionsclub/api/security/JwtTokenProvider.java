package com.lionsclub.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lionsclub.api.domain.user.Role;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        this.verifier = JWT.require(algorithm).build();
    }

    public java.time.Duration getDefaultExpiration() {
        return jwtConfig.getExpiration();
    }

    public java.time.Duration getRememberMeExpiration() {
        return jwtConfig.getRememberMeExpiration();
    }

    public String createToken(UUID userId, String email, Role role, String firstName, String lastName) {
        return createToken(userId, email, role, firstName, lastName, jwtConfig.getExpiration());
    }

    public String createToken(UUID userId, String email, Role role, String firstName, String lastName, java.time.Duration expiration) {
        var now = Instant.now();
        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", role.name())
                .withClaim("email", email)
                .withClaim("firstName", firstName)
                .withClaim("lastName", lastName)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(expiration))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    // Legacy method for backward compatibility with existing tests
    public String generateToken(UUID userId, Role role) {
        return createToken(userId, "", role, "", "");
    }
}