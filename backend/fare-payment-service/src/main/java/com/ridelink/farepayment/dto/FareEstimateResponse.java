package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.service.FareCalculator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** A quote only - nothing is stored. */
@Getter
@Builder
@AllArgsConstructor
public class FareEstimateResponse {
    private Double distanceKm;
    private Integer durationMinutes;
    private boolean durationEstimated;
    private Double baseFare;
    private Double distanceFare;
    private Double timeFare;
    private Double subtotal;
    private Double minimumFare;
    private boolean minimumFareApplied;
    private Double totalFare;
    private String currency;
    private String rule;

    public static FareEstimateResponse from(FareCalculator.Breakdown b) {
        return FareEstimateResponse.builder()
                .distanceKm(b.distanceKm())
                .durationMinutes(b.durationMinutes())
                .durationEstimated(b.durationEstimated())
                .baseFare(b.baseFare())
                .distanceFare(b.distanceFare())
                .timeFare(b.timeFare())
                .subtotal(b.subtotal())
                .minimumFare(b.minimumFare())
                .minimumFareApplied(b.minimumFareApplied())
                .totalFare(b.totalFare())
                .currency(b.currency())
                .rule(FareCalculator.RULE)
                .build();
    }
}
