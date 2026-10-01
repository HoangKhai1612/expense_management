package com.finai.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status, Pageable pageable);

    Optional<Feedback> findByIdAndUserId(Long id, Long userId);

    long countByStatus(FeedbackStatus status);
}
