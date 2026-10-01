package com.finai.user;

/** Account lifecycle state. Only ACTIVE accounts may authenticate. */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    DEACTIVATED
}
