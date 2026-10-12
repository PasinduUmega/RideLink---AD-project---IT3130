package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank
    private String driverId;

    @NotBlank
    private String make;

    @NotBlank
    private String model;

    private Integer year;

    @NotBlank
    private String plateNumber;

    private String color;

    @Positive
    private Integer capacity;
}
