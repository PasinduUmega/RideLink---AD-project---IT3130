package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/** Optional body for completing a ride - actual trip figures override the estimates. */
@Getter
@Setter
public class CompleteRideRequest {
    @Positive
    private Double actualDistanceKm;

    @PositiveOrZero
    private Integer actualDurationMinutes;
}
