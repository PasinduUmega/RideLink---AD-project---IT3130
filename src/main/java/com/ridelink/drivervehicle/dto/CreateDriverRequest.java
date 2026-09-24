package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateDriverRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String licenseNumber;

    @Future
    private LocalDate licenseExpiry;
}
