package com.ridelink.ridemanagement.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    private String id;

    /** References account-service User.id (MongoDB ObjectId) */
    @Indexed
    private String riderId;

    /** References driver-vehicle-service Driver.id (MongoDB ObjectId) - null until accepted */
    @Indexed
    private String driverId;

    private String pickupLocation;

    private String dropoffLocation;

    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;

    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    private Double distanceKm;

    private Double estimatedFare;

    private String cancellationReason;

    private Instant requestedAt;

    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    private Instant updatedAt;
}
