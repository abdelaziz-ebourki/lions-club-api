package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.EventService;
import com.lionsclub.api.web.dto.EventRequest;
import com.lionsclub.api.web.dto.EventResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private static final String OK = "200";

    private final EventService eventService;

    @Operation(summary = "List all events",
            description = "Public endpoint. Optionally filter by status (upcoming, ongoing, past).")
    @ApiResponse(responseCode = OK, description = "List of events",
            content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid status filter")
    @GetMapping
    public ResponseEntity<?> listEvents(@RequestParam(required = false) String status) {
        var events = eventService.listEvents(status);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Get single event",
            description = "Public endpoint. Returns event details by ID.")
    @ApiResponse(responseCode = OK, description = "Event details",
            content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @ApiResponse(responseCode = "404", description = "Event not found")
    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable UUID id) {
        var event = eventService.getEvent(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    @Operation(summary = "Create an event",
            description = "Admin only. Creates a new event with the provided details.")
    @ApiResponse(responseCode = "201", description = "Event created",
            content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        var event = eventService.createEvent(principal.userId(), request);
        return ResponseEntity.status(201).body(event);
    }

    @Operation(summary = "Update an event",
            description = "Admin only. Full-object replacement of an existing event.")
    @ApiResponse(responseCode = OK, description = "Event updated",
            content = @Content(schema = @Schema(implementation = EventResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        var event = eventService.updateEvent(id, request, principal.userId(), principal.role());
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(event);
    }

    @Operation(summary = "Delete an event",
            description = "Admin only. Removes an event from the system.")
    @ApiResponse(responseCode = OK, description = "Event deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        var deleted = eventService.deleteEvent(id, principal.userId(), principal.role());
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}