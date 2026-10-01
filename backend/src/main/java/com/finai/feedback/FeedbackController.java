package com.finai.feedback;

import com.finai.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "Tickets raised by users and tracked by administrators")
public class FeedbackController {

    private final FeedbackTicketService feedbackTicketService;

    public FeedbackController(FeedbackTicketService feedbackTicketService) {
        this.feedbackTicketService = feedbackTicketService;
    }

    @PostMapping
    @Operation(summary = "Submit a feedback ticket")
    public ResponseEntity<FeedbackTicketService.FeedbackView> create(
            @Valid @RequestBody FeedbackTicketService.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackTicketService.create(request));
    }

    @GetMapping
    @Operation(summary = "List the caller's own tickets with their current status")
    public PageResponse<FeedbackTicketService.FeedbackView> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return feedbackTicketService.listMine(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Return one of the caller's own tickets")
    public FeedbackTicketService.FeedbackView getMine(@PathVariable Long id) {
        return feedbackTicketService.getMine(id);
    }
}
