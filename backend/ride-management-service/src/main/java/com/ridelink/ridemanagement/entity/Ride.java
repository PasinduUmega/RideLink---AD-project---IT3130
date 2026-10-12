package com.ridelink.ridemanagement.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    /** Used to match drivers in the same service area, e.g. "Colombo". */
    private String serviceArea;

    @Builder.Default
    private Integer passengerCount = 1;

    /** Drivers who declined this ride - skipped by auto-assignment. */
    @Builder.Default
    private List<String> declinedDriverIds = new ArrayList<>();

    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    private Double distanceKm;

    private Double estimatedFare;

    private Integer durationMinutes;

    /** Set when fare-payment-service calculates the final fare on completion. */
    private String fareId;
    private Double finalFare;

    private String cancellationReason;

    /** Role of the user who cancelled (RIDER / DRIVER / ADMIN). */
    private String cancelledBy;

    private Instant requestedAt;

    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    private Instant updatedAt;

    /** Never null, including for rides stored before this field existed. */
    public List<String> getDeclinedDriverIds() {
        if (declinedDriverIds == null) declinedDriverIds = new ArrayList<>();
        return declinedDriverIds;
    }
}
