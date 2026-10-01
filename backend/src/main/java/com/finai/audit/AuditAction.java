package com.finai.audit;

/** Stable action keys written to the audit trail. */
public final class AuditAction {

    public static final String ADMIN_LOCK_USER = "ADMIN_LOCK_USER";
    public static final String ADMIN_UNLOCK_USER = "ADMIN_UNLOCK_USER";
    public static final String ADMIN_UPDATE_USER_STATUS = "ADMIN_UPDATE_USER_STATUS";
    public static final String ADMIN_UPDATE_CATEGORY = "ADMIN_UPDATE_CATEGORY";
    public static final String ADMIN_CREATE_CATEGORY = "ADMIN_CREATE_CATEGORY";
    public static final String ADMIN_UPDATE_FEEDBACK = "ADMIN_UPDATE_FEEDBACK";
    public static final String ADMIN_DENIED = "ADMIN_DENIED";

    private AuditAction() {
    }
}
