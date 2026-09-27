package com.ridelink.farepayment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateFareRequest {

    @NotNull
    @PositiveOrZero
    private Double distanceKm;

    /** Optional - estimated from distance at the configured average speed when omitted. */
    @PositiveOrZero
    private Integer durationMinutes;
}
