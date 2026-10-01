package com.finai.feedback;

import com.finai.common.ApiException;
import com.finai.common.PageResponse;
import com.finai.security.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** End-user side of the feedback flow: raise a ticket and follow its status. */
@Service
public class FeedbackTicketService {

    private final FeedbackRepository feedbackRepository;
    private final CurrentUserService currentUserService;

    public FeedbackTicketService(FeedbackRepository feedbackRepository,
                                CurrentUserService currentUserService) {
        this.feedbackRepository = feedbackRepository;
        this.currentUserService = currentUserService;
    }

    public record CreateRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 160, message = "Title must not exceed 160 characters")
            String title,

            @NotBlank(message = "Content is required")
            @Size(min = 10, max = 2000, message = "Content must be between 10 and 2000 characters")
            String content,

            FeedbackCategory category
    ) {
    }

    public record FeedbackView(
            Long id,
            String title,
            String content,
            FeedbackCategory category,
            FeedbackStatus status,
            String adminReply,
            Instant resolvedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        static FeedbackView of(Feedback feedback) {
            return new FeedbackView(feedback.getId(), feedback.getTitle(), feedback.getContent(),
                    feedback.getCategory(), feedback.getStatus(), feedback.getAdminReply(),
                    feedback.getResolvedAt(), feedback.getCreatedAt(), feedback.getUpdatedAt());
        }
    }

    @Transactional
    public FeedbackView create(CreateRequest request) {
        Long userId = currentUserService.requireUserId();
        Feedback feedback = new Feedback(userId, request.title().trim(), request.content().trim(),
                request.category() == null ? FeedbackCategory.OTHER : request.category());
        return FeedbackView.of(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackView> listMine(int page, int size) {
        Long userId = currentUserService.requireUserId();
        Page<Feedback> result = feedbackRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 100)));
        return PageResponse.of(result, FeedbackView::of);
    }

    @Transactional(readOnly = true)
    public FeedbackView getMine(Long id) {
        Long userId = currentUserService.requireUserId();
        Feedback feedback = feedbackRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("FEEDBACK_NOT_FOUND",
                        "No feedback ticket with id " + id + " belongs to you."));
        return FeedbackView.of(feedback);
    }
}
