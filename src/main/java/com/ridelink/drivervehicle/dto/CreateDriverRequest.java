package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Driver registration input; user ID and license are required, and any expiry must be in the future. */
@Getter
@Setter
public class CreateDriverRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String licenseNumber;

    @Future
    private LocalDate licenseExpiry;

    /** City / zone the driver operates in, e.g. "Colombo". */
    private String serviceArea;
}
