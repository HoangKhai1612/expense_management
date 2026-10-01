package com.finai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI engine configuration.
 *
 * The default engine is LOCAL: a deterministic analyst that answers strictly from
 * a database snapshot taken for the authenticated user. An external provider can
 * be switched on for a natural-language layer, but only as a rephrasing step over
 * those same verified facts, never as a source of numbers.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    public enum Provider {
        /** Built-in deterministic analyst. Always available, never needs a network call. */
        LOCAL,
        /** Any OpenAI-compatible chat completions endpoint. */
        OPENAI_COMPATIBLE
    }

    private Provider provider = Provider.LOCAL;

    /** API key for the external provider. Blank means the provider is not usable. */
    private String apiKey;

    private String baseUrl = "https://api.openai.com/v1";

    private String model = "gpt-4o-mini";

    private Duration timeout = Duration.ofSeconds(20);

    /** Refuse to load more than this many of the user's transactions into a prompt. */
    private int maxTransactionsInContext = 200;

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxTransactionsInContext() {
        return maxTransactionsInContext;
    }

    public void setMaxTransactionsInContext(int maxTransactionsInContext) {
        this.maxTransactionsInContext = maxTransactionsInContext;
    }

    /** True when an external provider is selected and actually configured. */
    public boolean isExternalProviderConfigured() {
        return provider != Provider.LOCAL
                && apiKey != null
                && !apiKey.isBlank()
                && baseUrl != null
                && !baseUrl.isBlank();
    }
}
