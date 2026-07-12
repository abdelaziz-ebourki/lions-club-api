package com.lionsclub.api.web;

import com.lionsclub.api.security.AuthService;
import com.lionsclub.api.security.JwtConfig;
import com.lionsclub.api.web.dto.AuthResponse;
import com.lionsclub.api.web.dto.LoginRequest;
import com.lionsclub.api.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.email(), request.password());
        if (result.success()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, createAuthCookie(result.token(), jwtConfig.getExpiration()))
                    .body(new AuthResponse("Login successful"));
        }
        return ResponseEntity.status(401)
                .body(java.util.Map.of("error", result.error()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(
                request.email(), request.password(),
                request.firstName(), request.lastName());
        if (result.success()) {
            return ResponseEntity.status(201)
                    .header(HttpHeaders.SET_COOKIE, createAuthCookie(result.token(), jwtConfig.getExpiration()))
                    .body(new AuthResponse("Registration successful"));
        }
        return ResponseEntity.status(409)
                .body(java.util.Map.of("error", result.error()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAuthCookie("", Duration.ZERO))
                .body(new AuthResponse("Logout successful"));
    }

    private String createAuthCookie(String token, Duration maxAge) {
        var builder = ResponseCookie.from("auth_token", token)
                .httpOnly(true)
                .secure(jwtConfig.isSecure())
                .sameSite("Lax")
                .path("/");
        if (maxAge != null) {
            builder.maxAge(maxAge);
        }
        return builder.build().toString();
    }
}
