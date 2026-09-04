package com.lionsclub.api.web;

import com.lionsclub.api.security.AuthService;
import com.lionsclub.api.security.JwtConfig;
import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.web.dto.AuthResponse;
import com.lionsclub.api.web.dto.ForgotPasswordRequest;
import com.lionsclub.api.web.dto.LoginRequest;
import com.lionsclub.api.web.dto.RegisterRequest;
import com.lionsclub.api.web.dto.ResetPasswordRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
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
            description = "Creates a new user account with display name, email and password. Returns auth_token cookie on success.")
    @ApiResponse(responseCode = "201", description = "Registration successful, auth_token cookie set")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request.email(), request.password(), request.name());
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

    @Operation(summary = "Resend verification email",
            description = "Issues a new email verification token for the authenticated user. The token is logged server-side until mail delivery is configured.")
    @ApiResponse(responseCode = OK, description = "Verification token issued")
    @ApiResponse(responseCode = "401", description = "Not authenticated or invalid token")
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        authService.resendVerification(principal.userId());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @Operation(summary = "Verify email address",
            description = "Verifies the authenticated user's email address using the token from the verification link.")
    @ApiResponse(responseCode = OK, description = "Email verified")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    @ApiResponse(responseCode = "401", description = "Not authenticated or invalid token")
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("token") String token) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var result = authService.verifyEmail(token);
        if (!result.ok()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @Operation(summary = "Request password reset",
            description = "Issues a password reset token for the given email. Always returns success to avoid leaking registered emails. The token is logged server-side until mail delivery is configured.")
    @ApiResponse(responseCode = OK, description = "Reset token issued if the email is registered")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email is registered, a reset link has been sent"));
    }

    @Operation(summary = "Reset password",
            description = "Sets a new password using the token from the reset link.")
    @ApiResponse(responseCode = OK, description = "Password reset")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords do not match")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "Passwords do not match"));
        }
        var result = authService.resetPassword(request.token(), request.password());
        if (!result.ok()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
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