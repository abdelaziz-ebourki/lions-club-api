package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.RsvpService;
import com.lionsclub.api.web.dto.RsvpRequest;
import com.lionsclub.api.web.dto.RsvpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    @Operation(summary = "RSVP to an event",
            description = "Authenticated members can RSVP (YES/NO/MAYBE) with optional plusOne and notes. Upsert — updates existing RSVP if one exists.")
    @ApiResponse(responseCode = "201", description = "RSVP created or updated (upsert)",
            content = @Content(schema = @Schema(implementation = RsvpResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input or event status")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "409", description = "Event at full capacity")
    @PostMapping("/rsvp")
    public ResponseEntity<?> createOrUpdateRsvp(@PathVariable UUID eventId,
                                                @Valid @RequestBody RsvpRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        try {
            var result = rsvpService.createOrUpdateRsvp(eventId, principal.userId(), request);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "List RSVPs for an event",
            description = "Admin only. Returns all RSVPs with member details.")
    @ApiResponse(responseCode = "200", description = "List of RSVPs",
            content = @Content(schema = @Schema(implementation = RsvpResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    @GetMapping("/rsvps")
    public ResponseEntity<?> getRsvps(@PathVariable UUID eventId) {
        var rsvps = rsvpService.getRsvpsForEvent(eventId);
        return ResponseEntity.ok(rsvps);
    }
}