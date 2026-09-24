package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DriverResponse {
    private String id;
    private String userId;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private DriverStatus status;
    private Double rating;
    private List<VehicleResponse> vehicles;
    private Instant createdAt;

    public static DriverResponse from(Driver d) {
        return from(d, List.of());
    }

    public static DriverResponse from(Driver d, List<VehicleResponse> vehicles) {
        return DriverResponse.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .licenseNumber(d.getLicenseNumber())
                .licenseExpiry(d.getLicenseExpiry())
                .status(d.getStatus())
                .rating(d.getRating())
                .vehicles(vehicles)
                .createdAt(d.getCreatedAt())
                .build();
    }
}
