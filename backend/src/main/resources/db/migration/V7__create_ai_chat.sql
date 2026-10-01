-- ============================================================
-- V7: AI conversations and messages
--   Every message row records whether the reply was produced from a
--   grounded database snapshot ("has_grounding_facts") so the audit trail
--   distinguishes a data-backed answer from a pure suggestion.
-- ============================================================

CREATE TABLE ai_conversations (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    title      VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE ai_messages (
    id                    BIGSERIAL PRIMARY KEY,
    conversation_id       BIGINT      NOT NULL,
    user_id               BIGINT      NOT NULL,
    role                  VARCHAR(16) NOT NULL,
    content               TEXT        NOT NULL,
    facts                 TEXT,
    engine                VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    latency_ms            BIGINT,
    has_grounding_facts   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_messages_user         FOREIGN KEY (user_id)         REFERENCES users (id)            ON DELETE CASCADE,
    CONSTRAINT ck_ai_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_ai_conversations_user ON ai_conversations (user_id, updated_at DESC);
CREATE INDEX idx_ai_messages_conversation ON ai_messages (conversation_id, id);
CREATE INDEX idx_ai_messages_user_created ON ai_messages (user_id, created_at DESC);

COMMENT ON TABLE  ai_messages                 IS 'Chat transcript rows. user_id is denormalised so an audit query never needs to join through the conversation.';
COMMENT ON COLUMN ai_messages.role           IS 'USER = question, ASSISTANT = reply, SYSTEM = operational notice such as a provider failure.';
COMMENT ON COLUMN ai_messages.facts         IS 'Newline-separated verified facts injected into the prompt / used to compose the answer.';
COMMENT ON COLUMN ai_messages.engine        IS 'Which engine produced the reply: LOCAL (built-in analyst) or an external provider id.';
COMMENT ON COLUMN ai_messages.has_grounding_facts IS 'FALSE when the answer carried no database fact, which also means no numeric claim was made.';
