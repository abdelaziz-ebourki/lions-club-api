package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.GalleryService;
import com.lionsclub.api.web.dto.GalleryResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final GalleryService galleryService;

    @Operation(summary = "Upload gallery image",
            description = "Stores an image and returns its public URLs. Admin only.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.status(201).body(galleryService.upload(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "List gallery items", description = "Returns gallery items with optional category/event filters, paginated.")
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "12") int limit,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "eventId", required = false) String eventId) {
        return ResponseEntity.ok(galleryService.search(category, eventId, page, limit));
    }

    @Operation(summary = "List all gallery items for admin", description = "Returns all items newest first. Admin only.")
    @GetMapping("/admin")
    public ResponseEntity<?> listForAdmin() {
        return ResponseEntity.ok(galleryService.listAllForAdmin());
    }

    @Operation(summary = "Get gallery item for admin", description = "Returns a single item by id. Admin only.")
    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getForAdmin(@PathVariable UUID id) {
        var item = galleryService.findById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Get gallery item by id", description = "Returns a single item by id.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        var item = galleryService.findById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Create gallery item", description = "Creates an item from an uploaded image URL. Admin only.")
    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GalleryResponse.GalleryItemRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var result = galleryService.create(principal.userId(), request);
        if (result.error() != null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.status(201).body(result.item());
    }

    @Operation(summary = "Update gallery item", description = "Partially updates an item. Admin only.")
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody GalleryResponse.GalleryItemRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var result = galleryService.update(id, request);
        if (!result.found()) {
            return ResponseEntity.notFound().build();
        }
        if (result.error() != null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.ok(result.item());
    }

    @Operation(summary = "Delete gallery item", description = "Deletes an item by id. Admin only.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!galleryService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
