package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRideRequest {

    @NotBlank
    private String riderId;

    @NotBlank
    private String pickupLocation;

    @NotBlank
    private String dropoffLocation;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double pickupLat;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double pickupLng;
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double dropoffLat;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double dropoffLng;

    /** Optional - derived from the coordinates when omitted. */
    @Positive
    private Double distanceKm;

    /** Optional - e.g. "Colombo"; narrows driver matching to that area. */
    private String serviceArea;

    @Min(1) @Max(8)
    private Integer passengerCount;
}
