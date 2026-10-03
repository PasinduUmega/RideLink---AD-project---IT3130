package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Requires an availability value; the service checks eligibility before allowing AVAILABLE. */
@Getter
@Setter
public class UpdateAvailabilityRequest {
    @NotNull
    private AvailabilityStatus availability;
}
