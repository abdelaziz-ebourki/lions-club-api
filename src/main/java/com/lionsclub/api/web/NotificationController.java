package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final NotificationService notificationService;

    @Operation(summary = "List own notifications",
            description = "Returns the authenticated user's notifications newest first with the unread count.")
    @GetMapping
    public ResponseEntity<?> inbox(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        return ResponseEntity.ok(notificationService.inbox(principal.userId(), limit));
    }

    @Operation(summary = "Mark notification as read", description = "Marks one of the authenticated user's notifications as read.")
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        if (!notificationService.markRead(principal.userId(), id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(summary = "Mark all notifications as read",
            description = "Marks all of the authenticated user's notifications as read.")
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        notificationService.markAllRead(principal.userId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
