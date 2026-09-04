package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.news.NewsArticle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<NewsArticle, UUID> {

    Optional<NewsArticle> findBySlug(String slug);

    Optional<NewsArticle> findBySlugAndStatus(String slug, String status);

    Page<NewsArticle> findByStatusOrderByPublishedAtDescCreatedAtDesc(String status, Pageable pageable);

    List<NewsArticle> findByStatusOrderByPublishedAtDescCreatedAtDesc(String status);

    List<NewsArticle> findAllByOrderByUpdatedAtDesc();

    boolean existsBySlug(String slug);
}
