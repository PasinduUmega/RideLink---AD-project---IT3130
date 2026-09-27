package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.entity.Fare;
import com.ridelink.farepayment.entity.FareStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class FareResponse {
    private String id;
    private String rideId;
    private String riderId;
    private Double baseFare;
    private Double distanceKm;
    private Double distanceFare;
    private Integer durationMinutes;
    private Double timeFare;
    private Double subtotal;
    private boolean minimumFareApplied;
    private Double totalFare;
    private String currency;
    private FareStatus status;
    private Instant createdAt;

    public static FareResponse from(Fare f) {
        return FareResponse.builder()
                .id(f.getId())
                .rideId(f.getRideId())
                .riderId(f.getRiderId())
                .baseFare(f.getBaseFare())
                .distanceKm(f.getDistanceKm())
                .distanceFare(f.getDistanceFare())
                .durationMinutes(f.getDurationMinutes())
                .timeFare(f.getTimeFare())
                .subtotal(f.getSubtotal())
                .minimumFareApplied(f.isMinimumFareApplied())
                .totalFare(f.getTotalFare())
                .currency(f.getCurrency())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
