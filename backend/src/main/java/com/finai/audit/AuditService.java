package com.finai.audit;

import com.finai.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public record AuditView(
            Long id,
            Long adminId,
            String adminName,
            String action,
            String targetType,
            Long targetId,
            String result,
            String detail,
            String ipAddress,
            java.time.Instant createdAt
    ) {
        static AuditView of(AuditLog entry) {
            return new AuditView(entry.getId(), entry.getAdminId(), entry.getAdminName(),
                    entry.getAction(), entry.getTargetType(), entry.getTargetId(),
                    entry.getResult(), entry.getDetail(), entry.getIpAddress(), entry.getCreatedAt());
        }
    }

    /**
     * Recorded in its own transaction so the trail survives even if the business
     * transaction that produced it is rolled back after the fact.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long adminId, String adminName, String action, String targetType,
                       Long targetId, String result, String detail) {
        try {
            auditLogRepository.save(new AuditLog(adminId, adminName, action, targetType, targetId,
                    result, detail, currentIp()));
            log.info("AUDIT action={} admin={} target={}/{} result={}",
                    action, adminName, targetType, targetId, result);
        } catch (RuntimeException ex) {
            // An audit write must never take down the operation it is describing.
            log.error("Failed to write audit entry for action {}", action, ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditView> list(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(Math.max(page, 0), size <= 0 ? 50 : Math.min(size, 200)))
                .map(AuditView::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditView> listPage(int page, int size) {
        return PageResponse.of(list(page, size));
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
