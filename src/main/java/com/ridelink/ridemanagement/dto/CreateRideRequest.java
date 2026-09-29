package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.NotBlank;
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

    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;

    private Double distanceKm;
}
