package com.lionsclub.api.service;

import com.lionsclub.api.domain.contact.ContactMessage;
import com.lionsclub.api.infrastructure.persistence.ContactRepository;
import com.lionsclub.api.web.dto.ContactResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private static final Set<String> STATUSES = Set.of("unread", "read", "archived");

    private final ContactRepository contactRepository;
    private final NotificationService notificationService;

    public List<ContactResponse> listAll() {
        return contactRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ContactService::toResponse)
                .toList();
    }

    @Transactional
    public ContactResponse submit(String name, String email, String subject, String message) {
        var contact = new ContactMessage();
        contact.setName(name.trim());
        contact.setEmail(email.trim());
        contact.setSubject(subject.trim());
        contact.setMessage(message.trim());
        contact.setStatus("unread");
        var saved = toResponse(contactRepository.save(contact));
        notificationService.notifyAdmins(
                NotificationService.TYPE_ADMIN_ANNOUNCEMENT,
                "New contact message",
                "From " + saved.name() + ": " + saved.subject(),
                "/admin/messages");
        return saved;
    }

    @Transactional
    public ContactResponse updateStatus(UUID id, String status) {
        if (status == null || !STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Status must be one of unread, read, archived");
        }
        var contact = contactRepository.findById(id).orElse(null);
        if (contact == null) {
            return null;
        }
        contact.setStatus(status.toLowerCase(Locale.ROOT));
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!contactRepository.existsById(id)) {
            return false;
        }
        contactRepository.deleteById(id);
        return true;
    }

    public static ContactResponse toResponse(ContactMessage contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getEmail(),
                contact.getSubject(),
                contact.getMessage(),
                contact.getCreatedAt(),
                contact.getStatus());
    }
}
