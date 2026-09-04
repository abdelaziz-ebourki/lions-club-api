package com.lionsclub.api.web;

import com.lionsclub.api.service.ContactService;
import com.lionsclub.api.web.dto.ContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private static final String ERROR_FIELD = "error";

    private final ContactService contactService;

    @Operation(summary = "Submit contact message", description = "Public contact form submission.")
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody ContactResponse.ContactRequest request) {
        var created = contactService.submit(request.name(), request.email(), request.subject(), request.message());
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "List contact messages", description = "Returns all messages newest first. Admin only.")
    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(contactService.listAll());
    }

    @Operation(summary = "Update message status", description = "Sets unread, read or archived. Admin only.")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @Valid @RequestBody ContactResponse.ContactStatusRequest request) {
        try {
            var updated = contactService.updateStatus(id, request.status());
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Delete message", description = "Deletes a message by id. Admin only.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!contactService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
