package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.gallery.GalleryItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GalleryRepository extends JpaRepository<GalleryItem, UUID> {

    List<GalleryItem> findAllByOrderByUploadedAtDesc();

    @Query("SELECT g FROM GalleryItem g WHERE (:category IS NULL OR g.category = :category)"
            + " AND (:eventId IS NULL OR g.event.id = :eventId) ORDER BY g.uploadedAt DESC")
    Page<GalleryItem> search(String category, UUID eventId, Pageable pageable);
}
