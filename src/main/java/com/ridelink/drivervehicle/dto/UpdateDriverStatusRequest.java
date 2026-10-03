package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Carries the required approval status for a driver status update. */
@Getter
@Setter
public class UpdateDriverStatusRequest {
    @NotNull
    private DriverStatus status;
}
