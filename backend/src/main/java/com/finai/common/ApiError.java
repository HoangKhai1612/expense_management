package com.finai.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Single error envelope used by every failing endpoint.
 * Stack traces are never serialised into this object; unexpected exceptions are
 * logged server-side and reported to the client as a generic INTERNAL_ERROR.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null);
    }

    public static ApiError of(int status, String code, String message, String path,
                              List<FieldViolation> violations) {
        return new ApiError(Instant.now(), status, code, message, path,
                violations == null || violations.isEmpty() ? null : violations);
    }
}
