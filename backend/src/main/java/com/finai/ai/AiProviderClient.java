package com.finai.ai;

import com.finai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Optional OpenAI-compatible provider, used only to add a natural-language
 * commentary on top of an already verified answer.
 *
 * The safety property is structural rather than prompt-based: the FACT block is
 * produced locally and returned verbatim no matter what the model says, and the
 * model's own words are quarantined into a clearly labelled COMMENTARY section.
 * A model that invents a figure therefore cannot contaminate the data-backed part
 * of the reply. Any failure is swallowed and reported so the caller can fall back.
 */
@Component
public class AiProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AiProviderClient.class);

    private static final String SYSTEM_PROMPT = """
            You are a personal finance assistant.
            You will be given a question and a list of VERIFIED FACTS that were computed
            from the user's own database records.

            Rules you must follow:
            1. Never introduce a number, amount, date or percentage that is not present
               in the VERIFIED FACTS.
            2. Never speculate about data you were not given.
            3. Reply in at most three sentences of plain prose. No lists, no headings.
            4. If the facts do not answer the question, say the data is insufficient.

            Reply with the commentary only.
            """;

    private final AiProperties properties;

    public AiProviderClient(AiProperties properties) {
        this.properties = properties;
    }

    public record ProviderResult(boolean available, String commentary) {
        static ProviderResult unavailable() {
            return new ProviderResult(false, null);
        }
    }

    /**
     * @return a commentary string, or {@code available = false} when no provider is
     *         configured or the call failed. Callers must treat the LOCAL answer as
     *         authoritative in both cases.
     */
    public ProviderResult fetchCommentary(String question, String verifiedFacts) {
        if (!properties.isExternalProviderConfigured()) {
            return ProviderResult.unavailable();
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                    .requestFactory(requestFactory())
                    .build();

            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "temperature", 0,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content",
                                    "QUESTION:\n" + question + "\n\nVERIFIED FACTS:\n" + verifiedFacts)));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String commentary = extractText(response);
            if (commentary == null || commentary.isBlank()) {
                return ProviderResult.unavailable();
            }
            return new ProviderResult(true, commentary.trim());
        } catch (RuntimeException ex) {
            // A provider outage must never surface as a failed request; the LOCAL
            // answer is already sufficient on its own.
            log.warn("External AI provider call failed, continuing with the local analyst: {}",
                    ex.getMessage());
            return ProviderResult.unavailable();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object content = messageMap.get("content");
        return content instanceof String text ? text : null;
    }

    private org.springframework.http.client.ClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getTimeout().toMillis());
        factory.setReadTimeout((int) properties.getTimeout().toMillis());
        return factory;
    }

    /** Convenience for callers that only need to know whether a provider is wired up. */
    public Duration timeout() {
        return properties.getTimeout();
    }
}
