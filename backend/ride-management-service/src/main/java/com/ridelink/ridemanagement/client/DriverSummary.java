package com.ridelink.ridemanagement.client;

/** Subset of driver-vehicle-service's DriverResponse that this service needs. */
public record DriverSummary(
        String id,
        String userId,
        String status,
        String availability,
        String serviceArea,
        Double currentLat,
        Double currentLng,
        Double distanceKm) {

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isAvailable() {
        return "AVAILABLE".equals(availability);
    }
}
