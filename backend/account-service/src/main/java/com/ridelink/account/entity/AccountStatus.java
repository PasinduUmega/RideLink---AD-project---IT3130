package com.ridelink.account.entity;

/**
 * Lifecycle state of an account.
 * ACTIVE      - can log in and use the platform.
 * SUSPENDED   - temporarily blocked by an admin (e.g. under investigation); can be reactivated.
 * DEACTIVATED - closed by the user or an admin; can be reactivated by an admin.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
