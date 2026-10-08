package com.school.lessonplan.user;

/**
 * Trạng thái tài khoản người dùng.
 * PENDING_EMAIL → PENDING_APPROVAL → ACTIVE, và LOCKED
 */
public enum UserStatus {
    PENDING_EMAIL,
    PENDING_APPROVAL,
    ACTIVE,
    LOCKED
}
