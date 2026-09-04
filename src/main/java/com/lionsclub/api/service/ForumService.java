package com.lionsclub.api.service;

import com.lionsclub.api.domain.forum.ForumCategory;
import com.lionsclub.api.domain.forum.ForumReply;
import com.lionsclub.api.domain.forum.ForumThread;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.ForumCategoryRepository;
import com.lionsclub.api.infrastructure.persistence.ForumReplyRepository;
import com.lionsclub.api.infrastructure.persistence.ForumThreadRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.ForumResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumService {

    private static final Set<String> STATUSES = Set.of("active", "pinned", "locked", "archived", "normal");

    private final ForumCategoryRepository categoryRepository;
    private final ForumThreadRepository threadRepository;
    private final ForumReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<ForumResponse.ForumCategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private ForumResponse.ForumCategoryResponse toCategoryResponse(ForumCategory category) {
        var threads = threadRepository.findByCategoryIdOrderByLastActivityDesc(category.getId());
        long postCount = threads.size() + threads.stream()
                .mapToLong(thread -> replyRepository.countByThreadId(thread.getId()))
                .sum();
        return new ForumResponse.ForumCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                threads.size(),
                postCount,
                category.getIcon());
    }

    public Optional<List<ForumResponse>> listThreads(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            return Optional.empty();
        }
        return Optional.of(threadRepository.findByCategoryIdOrderByLastActivityDesc(categoryId).stream()
                .map(this::toThreadResponse)
                .toList());
    }

    @Transactional
    public ForumResponse createThread(UUID categoryId, UUID authorId, String title, String content) {
        var category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return null;
        }
        User author = userRepository.findById(authorId).orElse(null);
        var thread = new ForumThread();
        thread.setCategory(category);
        thread.setTitle(title.trim());
        thread.setContent(content.trim());
        thread.setAuthor(author);
        thread.setAuthorName(author != null ? author.getFirstName() + " " + author.getLastName() : "Anonymous");
        thread.setStatus("normal");
        thread.setLastActivity(LocalDateTime.now());
        var saved = toThreadResponse(threadRepository.save(thread));
        notificationService.notifyAdmins(
                NotificationService.TYPE_ADMIN_ANNOUNCEMENT,
                "New forum thread",
                title.trim(),
                "/forum/" + categoryId + "/" + saved.id());
        return saved;
    }

    @Transactional
    public ForumResponse getThread(UUID categoryId, UUID threadId) {
        var thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null || !thread.getCategory().getId().equals(categoryId)) {
            return null;
        }
        thread.setViewCount(thread.getViewCount() + 1);
        return toThreadResponse(threadRepository.save(thread));
    }

    public ForumResponse getThreadById(UUID threadId) {
        return threadRepository.findById(threadId).map(this::toThreadResponse).orElse(null);
    }

    @Transactional
    public ForumResponse updateStatus(UUID threadId, String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw new IllegalArgumentException("Status must be one of active, pinned, locked, archived, normal");
        }
        var thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null) {
            return null;
        }
        thread.setStatus(status);
        return toThreadResponse(threadRepository.save(thread));
    }

    @Transactional
    public boolean deleteThread(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            return false;
        }
        threadRepository.deleteById(threadId);
        return true;
    }

    public Optional<List<ForumResponse.ForumReplyResponse>> listReplies(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            return Optional.empty();
        }
        return Optional.of(replyRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(ForumService::toReplyResponse)
                .toList());
    }

    @Transactional
    public ReplyResult createReply(UUID authorId, UUID threadId, String content, UUID parentReplyId) {
        var thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null) {
            return ReplyResult.notFound();
        }
        ForumReply parent = null;
        if (parentReplyId != null) {
            parent = replyRepository.findById(parentReplyId).orElse(null);
            if (parent == null || !parent.getThread().getId().equals(threadId)) {
                return ReplyResult.invalid("Parent reply not found in this thread");
            }
        }
        User author = userRepository.findById(authorId).orElse(null);
        var reply = new ForumReply();
        reply.setThread(thread);
        reply.setAuthor(author);
        reply.setAuthorName(author != null ? author.getFirstName() + " " + author.getLastName() : "Anonymous");
        reply.setContent(content.trim());
        reply.setParentReply(parent);
        var saved = toReplyResponse(replyRepository.save(reply));
        thread.setLastActivity(LocalDateTime.now());
        threadRepository.save(thread);
        if (thread.getAuthor() != null && !thread.getAuthor().getId().equals(authorId)) {
            notificationService.notify(
                    thread.getAuthor().getId(),
                    NotificationService.TYPE_FORUM_REPLY,
                    "New reply in your thread",
                    thread.getTitle(),
                    "/forum/" + thread.getCategory().getId() + "/" + thread.getId());
        }
        return ReplyResult.ok(saved);
    }

    private ForumResponse toThreadResponse(ForumThread thread) {
        return new ForumResponse(
                thread.getId(),
                thread.getCategory().getId(),
                thread.getTitle(),
                thread.getAuthorName(),
                thread.getContent(),
                thread.getCreatedAt(),
                thread.getStatus(),
                replyRepository.countByThreadId(thread.getId()),
                thread.getViewCount(),
                thread.getLastActivity());
    }

    private static ForumResponse.ForumReplyResponse toReplyResponse(ForumReply reply) {
        return new ForumResponse.ForumReplyResponse(
                reply.getId(),
                reply.getThread().getId(),
                reply.getAuthorName(),
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getUpdatedAt(),
                reply.getParentReply() != null ? reply.getParentReply().getId() : null);
    }

    public record ReplyResult(boolean found, String error, ForumResponse.ForumReplyResponse reply) {
        public static ReplyResult ok(ForumResponse.ForumReplyResponse reply) {
            return new ReplyResult(true, null, reply);
        }

        public static ReplyResult notFound() {
            return new ReplyResult(false, null, null);
        }

        public static ReplyResult invalid(String error) {
            return new ReplyResult(true, error, null);
        }
    }
}
