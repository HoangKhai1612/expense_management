package com.finai.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Optional<AiConversation> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
