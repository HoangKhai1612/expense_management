package com.finai.ai;

import com.finai.common.ApiException;
import com.finai.common.PageResponse;
import com.finai.config.AiProperties;
import com.finai.config.BudgetAlertProperties;
import com.finai.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Orchestrates an assistant turn end to end.
 *
 * Order of operations is deliberate: authenticate, build the caller's own fact
 * snapshot, derive the answer locally, only then optionally ask an external model
 * to comment on it. There is no path in this class that can write to the database
 * other than appending to the conversation transcript.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_QUESTION_LENGTH = 1000;
    private static final int TITLE_LENGTH = 80;

    private final AiDataSnapshotService snapshotService;
    private final AiProviderClient providerClient;
    private final AiProperties aiProperties;
    private final BudgetAlertProperties budgetAlertProperties;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final AiAnalyst analyst;

    public AiChatService(AiDataSnapshotService snapshotService,
                         AiProviderClient providerClient,
                         AiProperties aiProperties,
                         BudgetAlertProperties budgetAlertProperties,
                         AiConversationRepository conversationRepository,
                         AiMessageRepository messageRepository,
                         CurrentUserService currentUserService) {
        this.snapshotService = snapshotService;
        this.providerClient = providerClient;
        this.aiProperties = aiProperties;
        this.budgetAlertProperties = budgetAlertProperties;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.currentUserService = currentUserService;
        this.analyst = new AiAnalyst(budgetAlertProperties);
    }

    public record ChatRequest(
            Long conversationId,
            String message
    ) {
    }

    public record ChatResponse(
            Long conversationId,
            String question,
            String answer,
            List<String> facts,
            boolean grounded,
            String engine,
            boolean providerAvailable,
            String intent,
            long latencyMs,
            Instant createdAt
    ) {
    }

    public record MessageView(
            Long id,
            String role,
            String content,
            boolean hasGroundingFacts,
            Instant createdAt
    ) {
    }

    public record ConversationView(Long id, String title, Instant createdAt, Instant updatedAt) {
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Long userId = currentUserService.requireUserId();
        String question = requireQuestion(request.message());

        AiConversation conversation = resolveConversation(userId, request.conversationId(), question);

        messageRepository.save(new AiMessage(conversation.getId(), userId,
                AiMessage.ROLE_USER, question, null, "LOCAL", null, false));

        Instant startedAt = Instant.now();

        // The user id is passed explicitly and never taken from the prompt.
        AiDataSnapshotService.Snapshot snapshot = snapshotService.load(userId);

        boolean vietnamese = AiText.isVietnamese(question);
        AiIntent intent = AiText.detectIntent(question);
        AiAnalyst.Answer answer = analyst.answer(question, intent, snapshot, vietnamese);

        String engine = "LOCAL";
        boolean providerAvailable = false;
        String factsBlock = String.join("\n", answer.facts());

        if (answer.grounded() && !answer.facts().isEmpty()) {
            // Only verified answers are worth annotating. Asking a model to improve
            // a "not enough data" reply would only invite invention.
            AiProviderClient.ProviderResult commentary =
                    providerClient.fetchCommentary(question, factsBlock);
            if (commentary.available()) {
                engine = aiProperties.getProvider().name();
                providerAvailable = true;
                answer = new AiAnalyst.Answer(
                        appendCommentary(answer.content(), commentary.commentary(), vietnamese),
                        answer.facts(), answer.grounded());
            }
        }

        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

        AiMessage reply = messageRepository.save(new AiMessage(conversation.getId(), userId,
                AiMessage.ROLE_ASSISTANT, answer.content(), factsBlock, engine, latencyMs,
                answer.grounded()));

        conversation.rename(titleFor(question));
        conversationRepository.save(conversation);

        log.info("AI turn user={} conversation={} intent={} grounded={} engine={} latencyMs={}",
                userId, conversation.getId(), intent, answer.grounded(), engine, latencyMs);

        return new ChatResponse(conversation.getId(), question, answer.content(), answer.facts(),
                answer.grounded(), engine, providerAvailable, intent.name(), latencyMs,
                reply.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationView> listConversations(int page, int size) {
        Long userId = currentUserService.requireUserId();
        Page<AiConversation> result = conversationRepository.findByUserIdOrderByUpdatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 100)));
        return PageResponse.of(result, conversation -> new ConversationView(
                conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt()));
    }

    @Transactional(readOnly = true)
    public List<MessageView> messages(Long conversationId) {
        Long userId = currentUserService.requireUserId();
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND",
                        "No conversation with id " + conversationId + " belongs to you."));
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(message -> new MessageView(message.getId(), message.getRole(),
                        message.getContent(), message.isHasGroundingFacts(), message.getCreatedAt()))
                .toList();
    }

    private AiConversation resolveConversation(Long userId, Long conversationId, String question) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> ApiException.notFound("CONVERSATION_NOT_FOUND",
                            "No conversation with id " + conversationId + " belongs to you."));
        }
        return conversationRepository.save(new AiConversation(userId, titleFor(question)));
    }

    /**
     * The verified FACT block is inserted unchanged. Only an extra, clearly labelled
     * section is appended, so a hallucinated figure can never appear inside FACT.
     */
    private String appendCommentary(String verifiedAnswer, String commentary, boolean vietnamese) {
        return verifiedAnswer
                + "\n\nCOMMENTARY (" + (vietnamese ? "chưa kiểm chứng" : "not verified") + ")\n"
                + "- " + commentary.replace("\n", " ").trim();
    }

    private String requireQuestion(String message) {
        if (message == null || message.isBlank()) {
            throw ApiException.badRequest("QUESTION_REQUIRED", "A question is required.");
        }
        String trimmed = message.trim();
        if (trimmed.length() > MAX_QUESTION_LENGTH) {
            throw ApiException.badRequest("QUESTION_TOO_LONG",
                    "A question must not exceed " + MAX_QUESTION_LENGTH + " characters.");
        }
        return trimmed;
    }

    private String titleFor(String question) {
        String single = question.replaceAll("\\s+", " ").trim();
        if (single.length() <= TITLE_LENGTH) {
            return single;
        }
        return single.substring(0, TITLE_LENGTH - 1) + "…";
    }

    /** Exposed for the admin dashboard's AI usage metric. */
    @Transactional(readOnly = true)
    public long countQuestions(Long userId) {
        return messageRepository.countByUserIdAndRole(userId, AiMessage.ROLE_USER);
    }

    @Transactional(readOnly = true)
    public long countAllQuestions() {
        return messageRepository.count();
    }

    @Transactional(readOnly = true)
    public String engineLabel() {
        return aiProperties.getProvider().name()
                + (aiProperties.isExternalProviderConfigured() ? " (configured)" : " (local only)");
    }

    static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
