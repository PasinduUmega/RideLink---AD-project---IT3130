package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Defines the driver data returned to API clients, including vehicles and optional pickup distance. */
@Getter
@Builder
@AllArgsConstructor
public class DriverResponse {
    private String id;
    private String userId;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private DriverStatus status;
    private Double rating;
    private AvailabilityStatus availability;
    private String serviceArea;
    private Double currentLat;
    private Double currentLng;
    private Instant locationUpdatedAt;
    /** Distance from the requested pickup point - only set by the available-drivers lookup. */
    private Double distanceKm;
    private List<VehicleResponse> vehicles;
    private Instant createdAt;

    public static DriverResponse from(Driver d) {
        return from(d, List.of());
    }

    public static DriverResponse from(Driver d, List<VehicleResponse> vehicles) {
        // Convert the stored document to API data; older records without availability appear OFFLINE.
        return DriverResponse.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .licenseNumber(d.getLicenseNumber())
                .licenseExpiry(d.getLicenseExpiry())
                .status(d.getStatus())
                .rating(d.getRating())
                .availability(d.getAvailability() != null ? d.getAvailability() : AvailabilityStatus.OFFLINE)
                .serviceArea(d.getServiceArea())
                .currentLat(d.getCurrentLat())
                .currentLng(d.getCurrentLng())
                .locationUpdatedAt(d.getLocationUpdatedAt())
                .vehicles(vehicles)
                .createdAt(d.getCreatedAt())
                .build();
    }

    public static DriverResponse from(Driver d, List<VehicleResponse> vehicles, Double distanceKm) {
        // Add a calculated distance for available-driver searches without storing it in MongoDB.
        DriverResponse response = from(d, vehicles);
        response.distanceKm = distanceKm;
        return response;
    }
}
