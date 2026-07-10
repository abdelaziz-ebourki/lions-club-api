package com.lionsclub.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lionsclub.api.domain.user.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    public String generateToken(UUID userId, Role role) {
        var now = Instant.now();
        var algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", role.name())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(jwtConfig.getExpiration()))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) {
        try {
            var algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public UUID getUserIdFromToken(String token) {
        DecodedJWT decoded = validateToken(token);
        return UUID.fromString(decoded.getSubject());
    }

    public String getRoleFromToken(String token) {
        DecodedJWT decoded = validateToken(token);
        return decoded.getClaim("role").asString();
    }
}
