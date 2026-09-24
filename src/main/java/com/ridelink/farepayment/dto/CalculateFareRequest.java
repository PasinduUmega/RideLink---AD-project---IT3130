package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculateFareRequest {

    @NotBlank
    private String rideId;

    @NotBlank
    private String riderId;

    @NotNull
    @PositiveOrZero
    private Double distanceKm;
}
