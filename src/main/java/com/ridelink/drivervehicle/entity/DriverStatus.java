package com.ridelink.drivervehicle.entity;

/** Profile approval state; only APPROVED drivers can become AVAILABLE for ride matching. */
public enum DriverStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
