package com.finai.admin;

import com.finai.audit.AuditService;
import com.finai.common.PageResponse;
import com.finai.feedback.FeedbackStatus;
import com.finai.user.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Every route under /api/admin requires ROLE_ADMIN; SecurityConfig rejects
 * everything else with 403 before the controller is reached.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Privileged operations, restricted to ROLE_ADMIN")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final SystemMetricsService systemMetricsService;

    public AdminController(AdminService adminService, AuditService auditService,
                           SystemMetricsService systemMetricsService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.systemMetricsService = systemMetricsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Aggregated system metrics",
            description = "Every value is a live count from the database, except runtime counters which "
                    + "are explicitly labelled as covering the current process only.")
    public Map<String, Object> dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    @Operation(summary = "List, search and filter user accounts")
    public PageResponse<AdminService.AdminUserView> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listUsers(search, status, page, size);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Return one user account with its transaction count")
    public AdminService.AdminUserView user(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Lock, unlock or deactivate a user account",
            description = "A locked account is rejected at login and its existing tokens stop working "
                    + "on the next request. Administrators cannot change their own or another admin's status.")
    public AdminService.AdminUserView updateUserStatus(
            @PathVariable Long id,
            @RequestBody AdminService.UpdateUserStatusRequest request) {
        return adminService.updateUserStatus(id, request);
    }

    @GetMapping("/categories")
    @Operation(summary = "List the system category catalogue")
    public List<AdminService.AdminCategoryView> categories() {
        return adminService.listSystemCategories();
    }

    @PostMapping("/categories")
    @Operation(summary = "Add a category to the system catalogue")
    public AdminService.AdminCategoryView createCategory(
            @RequestBody AdminService.UpsertSystemCategoryRequest request) {
        return adminService.createSystemCategory(request);
    }

    @PatchMapping("/categories/{id}")
    @Operation(summary = "Rename, re-type or deactivate a system category",
            description = "Deactivation hides the category from pickers but keeps every existing "
                    + "transaction readable.")
    public AdminService.AdminCategoryView updateCategory(
            @PathVariable Long id,
            @RequestBody AdminService.UpsertSystemCategoryRequest request) {
        return adminService.updateSystemCategory(id, request);
    }

    @GetMapping("/feedback")
    @Operation(summary = "List feedback tickets, optionally filtered by status")
    public PageResponse<AdminService.AdminFeedbackView> feedback(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listFeedback(status, page, size);
    }

    @PatchMapping("/feedback/{id}")
    @Operation(summary = "Move a feedback ticket through its lifecycle")
    public AdminService.AdminFeedbackView updateFeedback(
            @PathVariable Long id,
            @RequestBody AdminService.UpdateFeedbackRequest request) {
        return adminService.updateFeedback(id, request);
    }

    @GetMapping("/system")
    @Operation(summary = "Process runtime metrics for the monitoring screen")
    public Map<String, Object> system() {
        return systemMetricsService.snapshot();
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Read the administrative audit trail, newest first")
    public PageResponse<AuditService.AuditView> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditService.listPage(page, size);
    }
}
