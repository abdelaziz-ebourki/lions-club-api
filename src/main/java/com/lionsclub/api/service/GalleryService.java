package com.lionsclub.api.service;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.gallery.GalleryItem;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.GalleryRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.GalleryResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private static final Set<String> CATEGORIES = Set.of("Event", "Project", "Team", "Community", "Partner");

    private final GalleryRepository galleryRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public GalleryResponse.UploadResponse upload(MultipartFile file) {
        String imageUrl = fileStorageService.store("gallery", file);
        return new GalleryResponse.UploadResponse(imageUrl, imageUrl);
    }

    public GalleryResponse.GalleryPage search(String category, String eventIdRaw, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        UUID eventId = parseUuid(eventIdRaw);
        String safeCategory = category == null || category.isBlank() ? null : category;
        var result = galleryRepository.search(safeCategory, eventId, PageRequest.of(safePage - 1, safeLimit));
        long total = result.getTotalElements();
        return new GalleryResponse.GalleryPage(
                result.getContent().stream().map(GalleryService::toResponse).toList(),
                total,
                safePage,
                safeLimit,
                (int) Math.ceil((double) total / safeLimit));
    }

    public GalleryResponse findById(UUID id) {
        return galleryRepository.findById(id).map(GalleryService::toResponse).orElse(null);
    }

    public List<GalleryResponse> listAllForAdmin() {
        return galleryRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(GalleryService::toResponse)
                .toList();
    }

    @Transactional
    public GalleryResult create(UUID uploaderId, GalleryResponse.GalleryItemRequest request) {
        String error = validate(request, true);
        if (error != null) {
            return GalleryResult.invalid(error);
        }
        Event event = resolveEvent(request.eventId());
        if (request.eventId() != null && event == null) {
            return GalleryResult.invalid("Event not found");
        }
        var item = new GalleryItem();
        apply(item, request, event);
        User uploader = userRepository.findById(uploaderId).orElse(null);
        item.setUploadedBy(uploader);
        return GalleryResult.ok(toResponse(galleryRepository.save(item)));
    }

    @Transactional
    public GalleryResult update(UUID id, GalleryResponse.GalleryItemRequest request) {
        var item = galleryRepository.findById(id).orElse(null);
        if (item == null) {
            return GalleryResult.notFound();
        }
        String error = validate(request, false);
        if (error != null) {
            return GalleryResult.invalid(error);
        }
        Event event = resolveEvent(request.eventId());
        if (request.eventId() != null && event == null) {
            return GalleryResult.invalid("Event not found");
        }
        apply(item, request, event);
        return GalleryResult.ok(toResponse(galleryRepository.save(item)));
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!galleryRepository.existsById(id)) {
            return false;
        }
        galleryRepository.deleteById(id);
        return true;
    }

    private void apply(GalleryItem item, GalleryResponse.GalleryItemRequest request, Event event) {
        item.setTitle(request.title().trim());
        item.setDescription(emptyToNull(request.description()));
        item.setCategory(request.category());
        item.setEvent(event);
        item.setTags(normalizeTags(request.tags()));
        item.setImageUrl(request.imageUrl());
        item.setThumbnailUrl(request.thumbnailUrl() == null || request.thumbnailUrl().isBlank()
                ? request.imageUrl() : request.thumbnailUrl());
    }

    private Event resolveEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId).orElse(null);
    }

    private static String validate(GalleryResponse.GalleryItemRequest request, boolean requireImage) {
        if (request == null || request.title() == null || request.title().trim().length() < 3
                || request.title().trim().length() > 200) {
            return "Title must be between 3 and 200 characters";
        }
        if (request.category() == null || !CATEGORIES.contains(request.category())) {
            return "Category must be one of " + String.join(", ", CATEGORIES);
        }
        if (requireImage && (request.imageUrl() == null || request.imageUrl().isBlank())) {
            return "Image is required";
        }
        return null;
    }

    static String[] normalizeTags(List<String> tags) {
        if (tags == null) {
            return new String[0];
        }
        var seen = new LinkedHashSet<String>();
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                seen.add(tag.trim());
            }
        }
        return seen.toArray(String[]::new);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static GalleryResponse toResponse(GalleryItem item) {
        return new GalleryResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getImageUrl(),
                item.getThumbnailUrl(),
                item.getCategory(),
                item.getEvent() != null ? item.getEvent().getId() : null,
                List.of(item.getTags() != null ? item.getTags() : new String[0]),
                item.getUploadedAt(),
                item.getUploadedBy() != null ? item.getUploadedBy().getId().toString() : null);
    }

    public record GalleryResult(boolean found, String error, GalleryResponse item) {
        public static GalleryResult ok(GalleryResponse item) {
            return new GalleryResult(true, null, item);
        }

        public static GalleryResult notFound() {
            return new GalleryResult(false, null, null);
        }

        public static GalleryResult invalid(String error) {
            return new GalleryResult(true, error, null);
        }
    }
}
