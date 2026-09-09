package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.ForumService;
import com.lionsclub.api.web.dto.ForumResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final ForumService forumService;

    @Operation(summary = "List forum categories", description = "Returns all categories with thread and post counts.")
    @GetMapping("/categories")
    public ResponseEntity<?> listCategories() {
        return ResponseEntity.ok(forumService.listCategories());
    }

    @Operation(summary = "List threads in a category", description = "Returns threads ordered by recent activity.")
    @GetMapping("/{categoryId}/threads")
    public ResponseEntity<?> listThreads(@PathVariable UUID categoryId) {
        var threads = forumService.listThreads(categoryId);
        if (threads.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(threads.get());
    }

    @Operation(summary = "Create thread", description = "Creates a thread in a category. Authenticated members only.")
    @PostMapping("/{categoryId}/threads")
    public ResponseEntity<?> createThread(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID categoryId,
            @Valid @RequestBody ForumResponse.CreateThreadRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        var thread = forumService.createThread(categoryId, principal.userId(), request.title(), request.content());
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(201).body(thread);
    }

    @Operation(summary = "Get thread in category", description = "Returns a thread and increments its view count.")
    @GetMapping("/{categoryId}/{threadId}")
    public ResponseEntity<?> getThread(@PathVariable UUID categoryId, @PathVariable UUID threadId) {
        var thread = forumService.getThread(categoryId, threadId);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(thread);
    }

    @Operation(summary = "Get thread by id", description = "Returns a thread by id.")
    @GetMapping("/threads/{id}")
    public ResponseEntity<?> getThreadById(@PathVariable UUID id) {
        var thread = forumService.getThreadById(id);
        if (thread == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(thread);
    }

    @Operation(summary = "Update thread status", description = "Sets active, pinned, locked, archived or normal. Admin only.")
    @PatchMapping("/threads/{id}")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ForumResponse.UpdateThreadStatusRequest request) {
        try {
            var thread = forumService.updateStatus(id, request.status());
            if (thread == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(thread);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Delete thread", description = "Deletes a thread and its replies. Admin only.")
    @DeleteMapping("/threads/{id}")
    public ResponseEntity<?> deleteThread(@PathVariable UUID id) {
        if (!forumService.deleteThread(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @Operation(summary = "List replies", description = "Returns a thread's replies oldest first.")
    @GetMapping("/replies")
    public ResponseEntity<?> listReplies(@RequestParam(value = "threadId", required = false) UUID threadId) {
        if (threadId == null) {
            return ResponseEntity.ok(java.util.List.of());
        }
        var replies = forumService.listReplies(threadId);
        if (replies.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(replies.get());
    }

    @Operation(summary = "Create reply", description = "Replies to a thread, optionally nested. Authenticated members only.")
    @PostMapping("/replies")
    public ResponseEntity<?> createReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ForumResponse.CreateReplyRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        if (request.threadId() == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "threadId is required"));
        }
        var result = forumService.createReply(principal.userId(), request.threadId(), request.content(), request.parentReplyId());
        if (!result.found()) {
            return ResponseEntity.notFound().build();
        }
        if (result.error() != null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
        }
        return ResponseEntity.status(201).body(result.reply());
    }

    @Operation(summary = "Delete reply", description = "Deletes a single reply. Admin only.")
    @DeleteMapping("/replies/{id}")
    public ResponseEntity<?> deleteReply(@PathVariable UUID id) {
        if (!forumService.deleteReply(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
