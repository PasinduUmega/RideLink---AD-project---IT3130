package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

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
