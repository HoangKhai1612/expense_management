package com.finai.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Append-only record of a privileged operation. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_name", length = 64)
    private String adminName;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "result", nullable = false, length = 16)
    private String result = "SUCCESS";

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
        // for JPA
    }

    public AuditLog(Long adminId, String adminName, String action, String targetType, Long targetId,
                    String result, String detail, String ipAddress) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.result = result;
        this.detail = truncate(detail);
        this.ipAddress = ipAddress;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }

    public Long getId() {
        return id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getResult() {
        return result;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
