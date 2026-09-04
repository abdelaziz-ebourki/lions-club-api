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
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
        try {
            var event = eventService.updateEvent(id, request, principal.userId());
            if (event == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Create an event from a form",
            description = "Admin only. Accepts multipart form fields (title, description, date, time, location, category, status, image file or URL) as sent by the admin event form.")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEventFromForm(MultipartHttpServletRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        var form = parseForm(request);
        if (form == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title, description, date, time, location and category are required"));
        }
        try {
            String imageUrl = eventService.storeImage(request.getFile("image"), trim(request.getParameter("image")), null);
            var event = eventService.createEvent(principal.userId(), form, imageUrl);
            return ResponseEntity.status(201).body(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Update an event from a form",
            description = "Admin only. Accepts multipart form fields. Omitted image keeps the current one.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEventFromForm(@PathVariable UUID id, MultipartHttpServletRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        var form = parseForm(request);
        if (form == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title, description, date, time, location and category are required"));
        }
        try {
            String imageParam = trim(request.getParameter("image"));
            var current = eventService.getEvent(id);
            if (current == null) {
                return ResponseEntity.notFound().build();
            }
            String imageUrl = eventService.storeImage(request.getFile("image"), imageParam, current.image());
            var event = eventService.updateEvent(id, form, principal.userId(), imageUrl, true);
            if (event == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private EventRequest parseForm(MultipartHttpServletRequest request) {
        String title = trim(request.getParameter("title"));
        String description = trim(request.getParameter("description"));
        String date = trim(request.getParameter("date"));
        String time = trim(request.getParameter("time"));
        String location = trim(request.getParameter("location"));
        String category = trim(request.getParameter("category"));
        if (title == null || title.length() < 3 || title.length() > 200
                || description == null || description.length() < 10 || description.length() > 2000
                || date == null || date.isBlank() || time == null || time.isBlank()
                || location == null || location.length() < 3 || location.length() > 200
                || category == null || category.isBlank()) {
            return null;
        }
        return new EventRequest(title, description, date, time, location, category, trim(request.getParameter("status")));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
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
        var deleted = eventService.deleteEvent(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}