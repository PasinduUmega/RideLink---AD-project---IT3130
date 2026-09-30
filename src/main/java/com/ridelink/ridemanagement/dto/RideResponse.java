package com.ridelink.ridemanagement.dto;

import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.entity.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class RideResponse {
    private String id;
    private String riderId;
    private String driverId;
    private String pickupLocation;
    private String dropoffLocation;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private RideStatus status;
    private Double distanceKm;
    private Double estimatedFare;
    private String cancellationReason;
    private Instant requestedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    public static RideResponse from(Ride r) {
        return RideResponse.builder()
                .id(r.getId())
                .riderId(r.getRiderId())
                .driverId(r.getDriverId())
                .pickupLocation(r.getPickupLocation())
                .dropoffLocation(r.getDropoffLocation())
                .pickupLat(r.getPickupLat())
                .pickupLng(r.getPickupLng())
                .dropoffLat(r.getDropoffLat())
                .dropoffLng(r.getDropoffLng())
                .status(r.getStatus())
                .distanceKm(r.getDistanceKm())
                .estimatedFare(r.getEstimatedFare())
                .cancellationReason(r.getCancellationReason())
                .requestedAt(r.getRequestedAt())
                .acceptedAt(r.getAcceptedAt())
                .startedAt(r.getStartedAt())
                .completedAt(r.getCompletedAt())
                .cancelledAt(r.getCancelledAt())
                .build();
    }
}
