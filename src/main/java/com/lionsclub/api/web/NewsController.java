package com.lionsclub.api.web;

import com.lionsclub.api.security.UserPrincipal;
import com.lionsclub.api.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private static final String ERROR_FIELD = "error";
    private static final String ERROR_UNAUTHORIZED = "Unauthorized";

    private final NewsService newsService;

    @Operation(summary = "List published news", description = "Returns published articles without content, paginated.")
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(newsService.listPublished(page, limit));
    }

    @Operation(summary = "List featured news", description = "Returns the 3 most recent published articles without content.")
    @GetMapping("/featured")
    public ResponseEntity<?> featured() {
        return ResponseEntity.ok(newsService.listFeatured());
    }

    @Operation(summary = "List all news for admin", description = "Returns all articles with content. Admin only.")
    @GetMapping("/admin")
    public ResponseEntity<?> listForAdmin() {
        return ResponseEntity.ok(newsService.listAllForAdmin());
    }

    @Operation(summary = "Get news article for admin", description = "Returns a single article by id. Admin only.")
    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getForAdmin(@PathVariable UUID id) {
        var article = newsService.findByIdForAdmin(id);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Get published article by slug", description = "Returns a published article with content.")
    @GetMapping("/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        var article = newsService.findPublishedBySlug(slug);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Create news article",
            description = "Creates an article from multipart form fields. Admin only.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @AuthenticationPrincipal UserPrincipal principal,
            MultipartHttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        NewsService.NewsInput input;
        try {
            input = parse(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
        if (input == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "Title, content, category and status are required"));
        }
        try {
            var result = newsService.create(principal.userId(), input, request.getFile("featuredImage"));
            if (result.error() != null) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
            }
            return ResponseEntity.status(201).body(result.article());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Update news article",
            description = "Updates an article from multipart form fields. Admin only.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            MultipartHttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_FIELD, ERROR_UNAUTHORIZED));
        }
        NewsService.NewsInput input;
        try {
            input = parse(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
        if (input == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, "Title, content, category and status are required"));
        }
        try {
            var result = newsService.update(id, input, request.getFile("featuredImage"));
            if (!result.found()) {
                return ResponseEntity.notFound().build();
            }
            if (result.error() != null) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, result.error()));
            }
            return ResponseEntity.ok(result.article());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_FIELD, e.getMessage()));
        }
    }

    @Operation(summary = "Delete news article", description = "Deletes an article by id. Admin only.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!newsService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    private NewsService.NewsInput parse(MultipartHttpServletRequest request) {
        String title = trim(request.getParameter("title"));
        String content = request.getParameter("content");
        String category = trim(request.getParameter("category"));
        String status = trim(request.getParameter("status"));
        if (title == null || content == null || category == null || status == null) {
            return null;
        }
        String publishedAtRaw = trim(request.getParameter("publishedAt"));
        try {
            return new NewsService.NewsInput(
                    title,
                    trim(request.getParameter("slug")),
                    content,
                    trim(request.getParameter("excerpt")),
                    category,
                    status,
                    NewsService.NewsInput.parsePublishedAt(publishedAtRaw),
                    trim(request.getParameter("featuredImage")));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("publishedAt must be a valid ISO datetime", e);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
