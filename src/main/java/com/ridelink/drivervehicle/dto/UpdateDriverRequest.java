package com.ridelink.drivervehicle.dto;

import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Optional profile changes; the service preserves existing values for fields left null. */
@Getter
@Setter
public class UpdateDriverRequest {
    private String licenseNumber;

    @Future
    private LocalDate licenseExpiry;

    private String serviceArea;
}
