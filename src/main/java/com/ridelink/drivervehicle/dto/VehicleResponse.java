package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** Defines vehicle data returned to API clients separately from the MongoDB document. */
@Getter
@Builder
@AllArgsConstructor
public class VehicleResponse {
    private String id;
    private String driverId;
    private String make;
    private String model;
    private Integer year;
    private String plateNumber;
    private String color;
    private Integer capacity;
    private boolean active;
    private Instant createdAt;

    public static VehicleResponse from(Vehicle v) {
        // Copy the stored vehicle's public response fields into a response object.
        return VehicleResponse.builder()
                .id(v.getId())
                .driverId(v.getDriverId())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .plateNumber(v.getPlateNumber())
                .color(v.getColor())
                .capacity(v.getCapacity())
                .active(v.isActive())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
