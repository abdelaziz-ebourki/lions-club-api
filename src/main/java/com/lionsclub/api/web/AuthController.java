package com.lionsclub.api.web;

import com.lionsclub.api.security.AuthService;
import com.lionsclub.api.security.JwtConfig;
import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.web.dto.AuthResponse;
import com.lionsclub.api.web.dto.LoginRequest;
import com.lionsclub.api.web.dto.RegisterRequest;
import com.lionsclub.api.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String OK = "200";
    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    @Operation(summary = "Authenticate user",
            description = "Validates email and password, returns auth_token cookie on success.")
    @ApiResponse(responseCode = OK, description = "Login successful, auth_token cookie set")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request.email(), request.password());
        if (result.success()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, createAuthCookie(result.token(), jwtConfig.getExpiration()))
                    .body(new AuthResponse("Login successful"));
        }
        return ResponseEntity.status(401)
                .body(Map.of(ERROR_FIELD, result.error()));
    }

    @Operation(summary = "Register a new user",
            description = "Creates a new user account with email, password, first name, and last name. Returns auth_token cookie on success.")
    @ApiResponse(responseCode = "201", description = "Registration successful, auth_token cookie set")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Email already registered")
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
                .body(Map.of(ERROR_FIELD, result.error()));
    }

    @Operation(summary = "Log out user",
            description = "Clears the auth_token cookie, ending the user session.")
    @ApiResponse(responseCode = OK, description = "Logout successful, auth_token cookie cleared")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAuthCookie("", Duration.ZERO))
                .body(new AuthResponse("Logout successful"));
    }

    @Operation(summary = "Get current user profile",
            description = "Returns the authenticated user's identity details. Requires valid auth_token cookie.")
    @ApiResponse(responseCode = OK, description = "User profile returned",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "401", description = "Not authenticated or invalid token")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var response = authService.getCurrentUser(principal.userId());
        if (response == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh auth token",
            description = "Issues a new auth_token cookie with a fresh expiry. Requires valid auth_token cookie.")
    @ApiResponse(responseCode = OK, description = "Token refreshed, new cookie set")
    @ApiResponse(responseCode = "401", description = "Not authenticated or invalid token")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var token = authService.refreshToken(principal.userId());
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAuthCookie(token, jwtConfig.getExpiration()))
                .body(new AuthResponse("Token refreshed"));
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