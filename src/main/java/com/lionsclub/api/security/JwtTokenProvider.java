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

    public String generateToken(UUID userId, Role role) {
        var now = Instant.now();
        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", role.name())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(jwtConfig.getExpiration()))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

}
