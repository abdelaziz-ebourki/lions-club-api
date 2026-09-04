package com.lionsclub.api.service;

import com.lionsclub.api.domain.news.NewsArticle;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.NewsRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.NewsResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NewsService {

    public static final String STATUS_PUBLISHED = "published";

    private static final Set<String> CATEGORIES = Set.of("Announcement", "News", "Event Recap", "Press Release");
    private static final Set<String> STATUSES = Set.of("draft", "published", "archived");

    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public NewsResponse.NewsPage listPublished(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        var result = newsRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(
                STATUS_PUBLISHED, PageRequest.of(safePage - 1, safeLimit));
        long total = result.getTotalElements();
        return new NewsResponse.NewsPage(
                result.getContent().stream().map(NewsService::toSummary).toList(),
                total,
                safePage,
                safeLimit,
                (int) Math.ceil((double) total / safeLimit));
    }

    public List<NewsResponse.NewsSummary> listFeatured() {
        return newsRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(STATUS_PUBLISHED).stream()
                .limit(3)
                .map(NewsService::toSummary)
                .toList();
    }

    public List<NewsResponse> listAllForAdmin() {
        return newsRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(NewsService::toResponse)
                .toList();
    }

    public NewsResponse findPublishedBySlug(String slug) {
        return newsRepository.findBySlugAndStatus(slug, STATUS_PUBLISHED)
                .map(NewsService::toResponse)
                .orElse(null);
    }

    public NewsResponse findByIdForAdmin(UUID id) {
        return newsRepository.findById(id).map(NewsService::toResponse).orElse(null);
    }

    @Transactional
    public NewsResult create(UUID authorId, NewsInput input, MultipartFile imageFile) {
        var validation = input.validate();
        if (validation != null) {
            return NewsResult.invalid(validation);
        }
        var author = userRepository.findById(authorId).orElse(null);
        var article = new NewsArticle();
        apply(article, input, resolveImage(null, input.featuredImageUrl(), imageFile));
        article.setSlug(uniqueSlug(baseSlug(input.slug(), input.title()), null));
        if (STATUS_PUBLISHED.equals(article.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        if (author != null) {
            article.setAuthor(author);
            article.setAuthorName(author.getFirstName() + " " + author.getLastName());
        }
        var saved = toResponse(newsRepository.save(article));
        if (STATUS_PUBLISHED.equals(saved.status())) {
            notificationService.notifyAll(
                    NotificationService.TYPE_ADMIN_ANNOUNCEMENT,
                    "New article published",
                    saved.title(),
                    "/news/" + saved.slug());
        }
        return NewsResult.ok(saved);
    }

    @Transactional
    public NewsResult update(UUID id, NewsInput input, MultipartFile imageFile) {
        var validation = input.validate();
        if (validation != null) {
            return NewsResult.invalid(validation);
        }
        var article = newsRepository.findById(id).orElse(null);
        if (article == null) {
            return NewsResult.notFound();
        }
        boolean wasPublished = STATUS_PUBLISHED.equals(article.getStatus());
        apply(article, input, resolveImage(article.getFeaturedImage(), input.featuredImageUrl(), imageFile));
        article.setSlug(uniqueSlug(baseSlug(input.slug(), input.title()), id));
        if (STATUS_PUBLISHED.equals(article.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        var saved = toResponse(newsRepository.save(article));
        if (STATUS_PUBLISHED.equals(saved.status()) && !wasPublished) {
            notificationService.notifyAll(
                    NotificationService.TYPE_ADMIN_ANNOUNCEMENT,
                    "New article published",
                    saved.title(),
                    "/news/" + saved.slug());
        }
        return NewsResult.ok(saved);
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!newsRepository.existsById(id)) {
            return false;
        }
        newsRepository.deleteById(id);
        return true;
    }

    private void apply(NewsArticle article, NewsInput input, String featuredImage) {
        article.setTitle(input.title());
        article.setContent(input.content());
        article.setExcerpt(input.excerpt());
        article.setCategory(input.category());
        article.setStatus(input.status().toLowerCase(Locale.ROOT));
        article.setFeaturedImage(featuredImage);
        article.setPublishedAt(input.publishedAt());
    }

    private String resolveImage(String current, String imageUrl, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return fileStorageService.store("news", imageFile);
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return current;
    }

    private String uniqueSlug(String base, UUID selfId) {
        String slug = base;
        int counter = 2;
        while (isSlugTaken(slug, selfId)) {
            slug = base + "-" + counter;
            counter++;
        }
        return slug;
    }

    private boolean isSlugTaken(String slug, UUID selfId) {
        var existing = newsRepository.findBySlug(slug).orElse(null);
        return existing != null && (selfId == null || !existing.getId().equals(selfId));
    }

    static String baseSlug(String slug, String title) {
        String source = slug != null && !slug.isBlank() ? slug : title;
        String slugified = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slugified.isEmpty() ? "article" : slugified;
    }

    static String stripHtml(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "").trim();
    }

    public static NewsResponse toResponse(NewsArticle article) {
        return new NewsResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getContent(),
                article.getExcerpt(),
                article.getFeaturedImage(),
                article.getCategory(),
                article.getStatus(),
                article.getAuthor() != null ? article.getAuthor().getId() : null,
                article.getAuthorName(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    public static NewsResponse.NewsSummary toSummary(NewsArticle article) {
        return new NewsResponse.NewsSummary(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getExcerpt(),
                article.getFeaturedImage(),
                article.getCategory(),
                article.getStatus(),
                article.getAuthor() != null ? article.getAuthor().getId() : null,
                article.getAuthorName(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    public record NewsInput(
            String title,
            String slug,
            String content,
            String excerpt,
            String category,
            String status,
            LocalDateTime publishedAt,
            String featuredImageUrl
    ) {
        String validate() {
            if (title == null || title.length() < 3 || title.length() > 200) {
                return "Title must be between 3 and 200 characters";
            }
            if (slug != null && !slug.isBlank()
                    && (slug.length() > 250 || !slug.matches("^[a-z0-9-]+$"))) {
                return "Slug must match ^[a-z0-9-]+$ and be at most 250 characters";
            }
            if (stripHtml(content).isEmpty()) {
                return "Content must not be empty";
            }
            if (excerpt != null && excerpt.length() > 500) {
                return "Excerpt must be at most 500 characters";
            }
            if (category == null || !CATEGORIES.contains(category)) {
                return "Category must be one of " + String.join(", ", CATEGORIES);
            }
            if (status == null || !STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
                return "Status must be one of draft, published, archived";
            }
            return null;
        }

        public static LocalDateTime parsePublishedAt(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (RuntimeException e) {
                return LocalDateTime.parse(raw);
            }
        }
    }

    public record NewsResult(boolean found, String error, NewsResponse article) {
        public static NewsResult ok(NewsResponse article) {
            return new NewsResult(true, null, article);
        }

        public static NewsResult notFound() {
            return new NewsResult(false, null, null);
        }

        public static NewsResult invalid(String error) {
            return new NewsResult(true, error, null);
        }
    }
}
