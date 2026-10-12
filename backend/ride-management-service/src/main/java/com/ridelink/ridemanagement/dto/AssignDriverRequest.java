package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDriverRequest {
    @NotBlank
    private String driverId;
}
