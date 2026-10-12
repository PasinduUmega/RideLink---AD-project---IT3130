package com.ridelink.drivervehicle.entity;

/**
 * Operational availability of a driver, independent of the approval {@link DriverStatus}.
 * OFFLINE   - not taking rides.
 * AVAILABLE - online and can be matched to a ride.
 * ON_TRIP   - currently assigned to / driving a ride.
 */
public enum AvailabilityStatus {
    OFFLINE,
    AVAILABLE,
    ON_TRIP
}
