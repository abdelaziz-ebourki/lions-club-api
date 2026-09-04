package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.UserService;
import com.lionsclub.api.web.dto.ChangePasswordRequest;
import com.lionsclub.api.web.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final UserService userService;

    @Operation(summary = "Get own profile",
            description = "Returns the authenticated user's profile details.")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var profile = userService.getProfile(principal.userId());
        if (profile == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update own profile",
            description = "Updates the authenticated user's display name and email.")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var result = userService.updateProfile(principal.userId(), request.name(), request.email());
        if (!result.present()) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        if (result.emailTaken()) {
            return ResponseEntity.status(409).body(Map.of(ERROR_FIELD, UserService.ERROR_DUPLICATE_EMAIL));
        }
        return ResponseEntity.ok(result.profile());
    }

    @Operation(summary = "Upload profile avatar",
            description = "Replaces the authenticated user's avatar. Accepts PNG, JPEG or WebP up to 5MB.")
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("avatar") MultipartFile avatar) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        try {
            var result = userService.updateAvatar(principal.userId(), avatar);
            if (!result.present()) {
                return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
            }
            return ResponseEntity.ok(result.profile());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Change own password",
            description = "Changes the authenticated user's password after verifying the current one.")
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var result = userService.changePassword(
                principal.userId(), request.currentPassword(), request.newPassword(), request.confirmPassword());
        if (!result.present()) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        if (!result.changed()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
