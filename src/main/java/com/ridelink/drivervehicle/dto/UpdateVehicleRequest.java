package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Editable vehicle details; null fields stay unchanged and supplied capacity must be positive. */
@Getter
@Setter
public class UpdateVehicleRequest {
    private String make;
    private String model;
    private Integer year;
    private String color;

    @Positive
    private Integer capacity;
}
