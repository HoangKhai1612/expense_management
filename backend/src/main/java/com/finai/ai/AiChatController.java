package com.finai.ai;

import com.finai.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Assistant",
        description = "Grounded financial analysis over the caller's own records. "
                + "The assistant cannot read another account and cannot write to the database.")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask a question about your own financial data",
            description = "The answer is built from a snapshot of the authenticated user's transactions "
                    + "and budgets. When there is not enough data the assistant says so instead of "
                    + "producing a number.")
    public AiChatService.ChatResponse chat(@RequestBody AiChatService.ChatRequest request) {
        return aiChatService.chat(request);
    }

    @GetMapping("/conversations")
    @Operation(summary = "List the caller's AI conversations, most recent first")
    public PageResponse<AiChatService.ConversationView> conversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return aiChatService.listConversations(page, size);
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Return the transcript of one of the caller's conversations")
    public List<AiChatService.MessageView> messages(@PathVariable Long id) {
        return aiChatService.messages(id);
    }
}
