package com.finai.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    long countByUserIdAndRole(Long userId, String role);

    long countByUserId(Long userId);
}
