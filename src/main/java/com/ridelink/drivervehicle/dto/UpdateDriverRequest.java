package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateDriverRequest {
    private String licenseNumber;

    @Future
    private LocalDate licenseExpiry;
}
