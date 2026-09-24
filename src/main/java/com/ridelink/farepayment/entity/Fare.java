package com.ridelink.farepayment.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "fares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fare {

    @Id
    private String id;

    /** References ride-management-service Ride.id (MongoDB ObjectId) */
    @Indexed(unique = true)
    private String rideId;

    /** References account-service User.id (the rider, a MongoDB ObjectId) */
    private String riderId;

    private Double baseFare;

    private Double distanceKm;

    private Double distanceFare;

    private Double totalFare;

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private FareStatus status = FareStatus.PENDING;

    private Instant createdAt;

    private Instant updatedAt;
}
