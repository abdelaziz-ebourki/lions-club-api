package com.lionsclub.api.service;

import com.lionsclub.api.domain.notification.Notification;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.NotificationRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.NotificationResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_FORUM_REPLY = "forum_reply";
    public static final String TYPE_EVENT_UPDATE = "event_update";
    public static final String TYPE_ADMIN_ANNOUNCEMENT = "admin_announcement";

    private static final Set<String> TYPES = Set.of(TYPE_FORUM_REPLY, TYPE_EVENT_UPDATE, TYPE_ADMIN_ANNOUNCEMENT);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationResponse.NotificationsResponse inbox(UUID userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<NotificationResponse> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(safeLimit)
                .map(NotificationService::toResponse)
                .toList();
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        return new NotificationResponse.NotificationsResponse(notifications, unreadCount);
    }

    @Transactional
    public boolean markRead(UUID userId, UUID notificationId) {
        var notification = notificationRepository.findByIdAndUserId(notificationId, userId).orElse(null);
        if (notification == null) {
            return false;
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public void markAllRead(UUID userId) {
        var unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void notify(UUID userId, String type, String title, String description, String targetUrl) {
        var user = userRepository.findById(userId).filter(User::isEnabled).orElse(null);
        if (user == null || !TYPES.contains(type)) {
            return;
        }
        var notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyAll(String type, String title, String description, String targetUrl) {
        userRepository.findAll().stream()
                .filter(User::isEnabled)
                .map(User::getId)
                .forEach(userId -> notify(userId, type, title, description, targetUrl));
    }

    @Transactional
    public void notifyAdmins(String type, String title, String description, String targetUrl) {
        userRepository.findAll().stream()
                .filter(user -> user.isEnabled() && user.getRole() == Role.ADMIN)
                .map(User::getId)
                .forEach(userId -> notify(userId, type, title, description, targetUrl));
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getTargetUrl(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
